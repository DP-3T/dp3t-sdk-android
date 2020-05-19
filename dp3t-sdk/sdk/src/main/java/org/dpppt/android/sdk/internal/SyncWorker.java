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

		try {
			doSync(context);
		} catch (Exception e) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.getMessage());
			return Result.retry();
		}
		Logger.d(TAG, "SyncWorker finished with success");
		return Result.success();
	}

	public static void doSync(Context context) throws Exception {
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
			Logger.e(TAG, e);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
			ErrorState syncError;
			if (e instanceof ServerTimeOffsetException) {
				syncError = ErrorState.SYNC_ERROR_TIMING;
			} else if (e instanceof SignatureException) {
				syncError = ErrorState.SYNC_ERROR_SIGNATURE;
			} else if (e instanceof StatusCodeException) {
				syncError = ErrorState.SYNC_ERROR_SERVER;
			} else if (e instanceof SQLiteException) {
				syncError = ErrorState.SYNC_ERROR_DATABASE;
			} else {
				syncError = ErrorState.SYNC_ERROR_NETWORK;
			}  //TODO check API exception sync errors
			SyncErrorState.getInstance().setSyncError(syncError);
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
			throw e;
		}
	}

	private static void doSyncInternal(Context context) throws Exception {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		ApplicationInfo appConfig = appConfigManager.getAppConfig();

		Exception lastException = null;
		HashMap<DayDate, Long> lastLoadedTimes = appConfigManager.getLastLoadedTimes();
		HashMap<DayDate, Long> lastExposureClientCalls = appConfigManager.getLastExposureClientCalls();

		BackendBucketRepository backendBucketRepository =
				new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);
		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

		DayDate lastDateToCheck = new DayDate();
		DayDate dateToLoad = lastDateToCheck.subtractDays(9);
		while (dateToLoad.isBeforeOrEquals(lastDateToCheck)) {

			if (lastExposureClientCalls.get(dateToLoad) == null ||
					lastExposureClientCalls.get(dateToLoad) < getLastDesiredSyncTime(dateToLoad)) {
				try {
					Response<ResponseBody> result =
							backendBucketRepository.getGaenExposees(dateToLoad, lastLoadedTimes.get(dateToLoad));

					if (result.code() != 204) {
						File file = new File(context.getCacheDir(),
								dateToLoad.formatAsString() + "_" + lastLoadedTimes.get(dateToLoad) + ".zip");
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
						byte[] bytesIn = new byte[1024];
						int read = 0;
						InputStream bodyStream = result.body().byteStream();
						while ((read = bodyStream.read(bytesIn)) != -1) {
							bos.write(bytesIn, 0, read);
						}
						bos.close();

						ArrayList<File> fileList = new ArrayList<>();
						fileList.add(file);
						String token = dateToLoad.formatAsString();
						lastExposureClientCalls.put(dateToLoad, System.currentTimeMillis());
						googleExposureClient.provideDiagnosisKeys(fileList, token);
					}

					lastLoadedTimes.put(dateToLoad, Long.parseLong(result.headers().get("x-published-until")));
				} catch (Exception e) {
					e.printStackTrace();
					lastException = e;
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
		dateIterator = lastExposureClientCalls.keySet().iterator();
		while (dateIterator.hasNext()) {
			if (dateIterator.next().isBefore(lastDateToKeep)) {
				dateIterator.remove();
			}
		}

		appConfigManager.setLastLoadedTimes(lastLoadedTimes);
		appConfigManager.setLastExposureClientCalls(lastExposureClientCalls);

		if (lastException != null) {
			throw lastException;
		} else {
			appConfigManager.setLastSyncDate(System.currentTimeMillis());
		}
	}

	private static long getLastDesiredSyncTime(DayDate dateToLoad) {
		if (BuildConfig.FLAVOR.equals("calibration")) {
			long now = System.currentTimeMillis();
			return now - (now % (5 * 60 * 1000l));
		} else {
			Calendar cal = new GregorianCalendar();
			if (cal.get(Calendar.HOUR_OF_DAY) < 6) {
				cal.add(Calendar.DATE, -1);
				cal.set(Calendar.HOUR_OF_DAY, 20);
			} else if (cal.get(Calendar.HOUR_OF_DAY) < 20) {
				cal.set(Calendar.HOUR_OF_DAY, 6);
			} else {
				cal.set(Calendar.HOUR_OF_DAY, 20);
			}
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis();
		}
	}

	private static void uploadPendingKeys(Context context) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		PendingKeyUploadStorage pendingKeyUploadStorage = PendingKeyUploadStorage.getInstance(context);
		PendingKeyUploadStorage.PendingKey pendingKey = null;
		try {
			while (pendingKeyUploadStorage.peekRollingStartNumber() < DateUtil.getCurrentRollingStartNumber()) {
				pendingKey = pendingKeyUploadStorage.popNextPendingKey();
				if (pendingKey.getRollingStartNumber() < DateUtil.getRollingStartNumberForDate(new DayDate().subtractDays(1))) {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (pendingKey != null) {
				pendingKeyUploadStorage.addPendingKey(pendingKey);
			}
		}
	}

}
