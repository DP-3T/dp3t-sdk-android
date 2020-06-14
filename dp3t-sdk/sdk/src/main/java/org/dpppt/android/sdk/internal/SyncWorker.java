/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.history.HistoryDatabase;
import org.dpppt.android.sdk.internal.history.HistoryEntry;
import org.dpppt.android.sdk.internal.history.HistoryEntryType;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.util.DateUtil;

import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class SyncWorker extends Worker {

	private static final String TAG = "SyncWorker";
	private static final String WORK_TAG = "org.dpppt.android.sdk.internal.SyncWorker";
	private static final String KEYFILE_PREFIX = "keyfile_";

	private static PublicKey bucketSignaturePublicKey;

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);

		Logger.d(TAG, "scheduled SyncWorker");
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelAllWorkByTag(WORK_TAG);
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	public static void setBucketSignaturePublicKey(PublicKey publicKey) {
		bucketSignaturePublicKey = publicKey;
	}

	@NonNull
	@Override
	public Result doWork() {
		Logger.d(TAG, "start SyncWorker");
		Context context = getApplicationContext();

		if (AppConfigManager.getInstance(context).getDevHistory()) {
			HistoryDatabase.getInstance(context)
					.addEntry(new HistoryEntry(HistoryEntryType.WORKER_STARTED, "Sync", true, System.currentTimeMillis()));
		}

		try {
			new SyncImpl(context).doSync();
		} catch (Exception e) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.getMessage());
			return Result.retry();
		}
		Logger.d(TAG, "SyncWorker finished with success");
		return Result.success();
	}

	public static class SyncImpl {

		Context context;
		long currentTime;

		public SyncImpl(Context context) {
			this(context, System.currentTimeMillis());
		}

		public SyncImpl(Context context, long currentTime) {
			this.context = context;
			this.currentTime = currentTime;
		}

		public void doSync() throws Exception {
			synchronized (SyncImpl.class) {
				GaenStateHelper.invalidateGaenAvailability(context);
				GaenStateHelper.invalidateGaenEnabled(context);

				try {
					uploadPendingKeys(context);
					doSyncInternal(context);
					Logger.i(TAG, "synced");
					AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
					SyncErrorState.getInstance().setSyncError(null);
					BroadcastHelper.sendUpdateAndErrorBroadcast(context);
				} catch (Exception e) {
					Logger.e(TAG, "sync", e);
					AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
					ErrorState syncError;
					if (e instanceof ServerTimeOffsetException) {
						syncError = ErrorState.SYNC_ERROR_TIMING;
					} else if (e instanceof SignatureException) {
						syncError = ErrorState.SYNC_ERROR_SIGNATURE;
					} else if (e instanceof StatusCodeException) {
						syncError = ErrorState.SYNC_ERROR_SERVER;
						syncError.setErrorCode("ASST" + ((StatusCodeException) e).getCode());
					} else if (e instanceof ApiException) {
						syncError = ErrorState.SYNC_ERROR_API_EXCEPTION;
						syncError.setErrorCode("AGAEN" + ((ApiException) e).getStatusCode());
					} else if (e instanceof SSLException) {
						syncError = ErrorState.SYNC_ERROR_SSLTLS;
					} else {
						syncError = ErrorState.SYNC_ERROR_NETWORK;
						syncError.setErrorCode(null);
					}
					SyncErrorState.getInstance().setSyncError(syncError);
					BroadcastHelper.sendUpdateAndErrorBroadcast(context);
					throw e;
				}
			}
		}

		private void doSyncInternal(Context context) throws Exception {
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			ApplicationInfo appConfig = appConfigManager.getAppConfig();

			Exception lastException = null;
			HashMap<DayDate, Long> lastLoadedTimes = appConfigManager.getLastLoadedTimes();
			HashMap<DayDate, Long> lastSyncCallTimes = appConfigManager.getLastSyncCallTimes();

			BackendBucketRepository backendBucketRepository =
					new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);
			GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

			DayDate lastDateToCheck = new DayDate(currentTime);
			DayDate dateToLoad = lastDateToCheck.subtractDays(9);
			int numInstantErrors = 0;
			int numDelayedErrors = 0;
			int numSuccesses = 0;
			while (dateToLoad.isBeforeOrEquals(lastDateToCheck)) {
				Long lastSynCallTime = lastSyncCallTimes.get(dateToLoad);
				if (lastSynCallTime == null) {
					// if there is no last sync call time recorded, set it to 5:59:59.999 on the current day, to make sure the
					// first sync happens after 6am, otherwise we risk running into the 20 calls ratelimit.
					Calendar cal = new GregorianCalendar();
					cal.setTimeInMillis(currentTime);
					cal.set(Calendar.HOUR_OF_DAY, 5);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					cal.set(Calendar.MILLISECOND, 999);
					lastSynCallTime = cal.getTimeInMillis();
					Logger.d(TAG, "never loaded before, set last sync time to 5:59:59 to prevent rate limit issues");
				}

				if (lastSynCallTime < getLastDesiredSyncTime(dateToLoad)) {
					try {
						Logger.d(TAG, "loading exposees for " + dateToLoad.formatAsString());
						Response<ResponseBody> result =
								backendBucketRepository.getGaenExposees(dateToLoad, lastLoadedTimes.get(dateToLoad));

						if (result.code() != 204) {
							File file = new File(context.getCacheDir(),
									KEYFILE_PREFIX + dateToLoad.formatAsString() + "_" + lastLoadedTimes.get(dateToLoad) + ".zip");
							try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
								byte[] bytesIn = new byte[1024];
								int read = 0;
								InputStream bodyStream = result.body().byteStream();
								while ((read = bodyStream.read(bytesIn)) != -1) {
									bos.write(bytesIn, 0, read);
								}
							}

							ArrayList<File> fileList = new ArrayList<>();
							fileList.add(file);
							String token = dateToLoad.formatAsString();
							Logger.d(TAG,
									"provideDiagnosisKeys for " + dateToLoad.formatAsString() + " with size " + file.length());
							googleExposureClient.provideDiagnosisKeys(fileList, token);
						}
						lastSyncCallTimes.put(dateToLoad, currentTime);
						lastLoadedTimes.put(dateToLoad, Long.parseLong(result.headers().get("x-published-until")));
						numSuccesses++;
					} catch (Exception e) {
						e.printStackTrace();
						lastException = e;
						if (isDelayedSyncError(e)) {
							numDelayedErrors++;
						} else {
							numInstantErrors++;
						}
					}
				}

				dateToLoad = dateToLoad.addDays(1);
			}

			DayDate lastDateToKeep = new DayDate().subtractDays(10);
			Iterator<DayDate> dateIterator = lastLoadedTimes.keySet().iterator();
			while (dateIterator.hasNext()) {
				if (dateIterator.next().isBefore(lastDateToKeep)) {
					dateIterator.remove();
				}
			}
			dateIterator = lastSyncCallTimes.keySet().iterator();
			while (dateIterator.hasNext()) {
				if (dateIterator.next().isBefore(lastDateToKeep)) {
					dateIterator.remove();
				}
			}

			cleanupOldKeyFiles(context);

			appConfigManager.setLastLoadedTimes(lastLoadedTimes);
			appConfigManager.setLastSyncCallTimes(lastSyncCallTimes);

			if (numInstantErrors > 0 || numDelayedErrors > 0 || numSuccesses > 0) {
				int base = 'A';
				String historyStatus =
						String.valueOf((char) (base + numInstantErrors)) + (char) (base + numDelayedErrors) +
								(char) (base + numSuccesses);
				HistoryDatabase.getInstance(context).addEntry(
						new HistoryEntry(HistoryEntryType.SYNC, historyStatus, lastException == null, System.currentTimeMillis()));
			}

			if (lastException != null) {
				throw lastException;
			} else {
				appConfigManager.setLastSyncDate(currentTime);
			}
		}

		private void cleanupOldKeyFiles(Context context) {
			for (File file : context.getCacheDir().listFiles()) {
				if (file.getName().startsWith(KEYFILE_PREFIX)) {
					if (!file.delete()) {
						Logger.w(TAG, "Unable to delete file " + file.getName());
					}
				}
			}
		}

		private long getLastDesiredSyncTime(DayDate dateToLoad) {
			if (BuildConfig.FLAVOR.equals("calibration")) {
				return currentTime - (currentTime % (5 * 60 * 1000L));
			} else {
				Calendar cal = new GregorianCalendar();
				cal.setTimeInMillis(currentTime);
				if (cal.get(Calendar.HOUR_OF_DAY) < 6) {
					cal.add(Calendar.DATE, -1);
					cal.set(Calendar.HOUR_OF_DAY, 18);
				} else if (cal.get(Calendar.HOUR_OF_DAY) < 18) {
					cal.set(Calendar.HOUR_OF_DAY, 6);
				} else {
					cal.set(Calendar.HOUR_OF_DAY, 18);
				}
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				return cal.getTimeInMillis();
			}
		}

		private boolean isDelayedSyncError(Exception e) {
			if (e instanceof ServerTimeOffsetException || e instanceof SignatureException || e instanceof StatusCodeException ||
					e instanceof SQLiteException || e instanceof ApiException || e instanceof SSLException) {
				return false;
			} else {
				return true;
			}
		}

		private void uploadPendingKeys(Context context) {
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			PendingKeyUploadStorage pendingKeyUploadStorage = PendingKeyUploadStorage.getInstance(context);
			PendingKeyUploadStorage.PendingKey pendingKey = null;
			try {
				int numPendingUploaded = 0;
				int numFakePendingUploaded = 0;
				while (pendingKeyUploadStorage.peekRollingStartNumber() <
						DateUtil.getRollingStartNumberForDate(new DayDate(currentTime))) {
					pendingKey = pendingKeyUploadStorage.popNextPendingKey();
					if (pendingKey.getRollingStartNumber() <
							DateUtil.getRollingStartNumberForDate(new DayDate(currentTime).subtractDays(1))) {
						//ignore pendingKeys older than one day, upload token will be invalid
						continue;
					}
					GaenKey gaenKey = null;
					if (!pendingKey.isFake()) {
						List<TemporaryExposureKey> keys =
								GoogleExposureClient.getInstance(context).getTemporaryExposureKeyHistorySynchronous();

						for (TemporaryExposureKey key : keys) {
							if (key.getRollingStartIntervalNumber() == pendingKey.getRollingStartNumber()) {
								gaenKey = new GaenKey(toBase64(key.getKeyData()),
										key.getRollingStartIntervalNumber(),
										key.getRollingPeriod(),
										key.getTransmissionRiskLevel());
								break;
							}
						}
						if (gaenKey == null) {
							//key for specified rollingStartNumber was not found, user must have cleared data
							continue;
						}
					} else {
						gaenKey = new GaenKey(toBase64(new byte[16]),
								pendingKey.getRollingStartNumber(),
								0,
								0,
								1);
					}
					appConfigManager.getBackendReportRepository(context).addPendingGaenKey(gaenKey, pendingKey.getToken());
					if (!pendingKey.isFake()) {
						DP3T.stop(context);
						appConfigManager.setIAmInfectedIsResettable(true);
					}
					if (pendingKey.isFake()) {
						numFakePendingUploaded++;
					} else {
						numPendingUploaded++;
					}
				}
				if (appConfigManager.getDevHistory() && (numFakePendingUploaded > 0 || numPendingUploaded > 0)) {
					HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
					int base = 'A';
					String status = String.valueOf((char) (base + numPendingUploaded)) + (char) (base + numFakePendingUploaded);
					historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.NEXT_DAY_KEY_UPLOAD_REQUEST, status, true,
							System.currentTimeMillis()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (pendingKey != null) {
					pendingKeyUploadStorage.addPendingKey(pendingKey);
					if (appConfigManager.getDevHistory()) {
						HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
						String status = e instanceof StatusCodeException ? String.valueOf(((StatusCodeException) e).getCode()) :
										"NETW";
						historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.NEXT_DAY_KEY_UPLOAD_REQUEST, status, false,
								System.currentTimeMillis()));
					}
				}
			}
		}

	}

}
