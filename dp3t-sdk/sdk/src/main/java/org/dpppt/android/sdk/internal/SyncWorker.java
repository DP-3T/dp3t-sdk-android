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
import androidx.annotation.NonNull;
import androidx.work.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.history.HistoryDatabase;
import org.dpppt.android.sdk.internal.history.HistoryEntry;
import org.dpppt.android.sdk.internal.history.HistoryEntryType;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateCache;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.storage.PendingKeyUploadStorage;
import org.dpppt.android.sdk.internal.storage.models.PendingKey;
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

	protected static final String KEY_BUNDLE_TAG_HEADER = "x-key-bundle-tag";

	private static final long SYNC_INTERVAL = BuildConfig.FLAVOR.equals("calibration") ? 5 * 60 * 1000L : 4 * 60 * 60 * 1000L;

	private static PublicKey bucketSignaturePublicKey;

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 120, TimeUnit.MINUTES)
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

		private final Context context;
		private final long currentTime;

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

					if (DP3T.isTracingEnabled(context) && !Boolean.FALSE.equals(GaenStateCache.isGaenEnabled())) {
						doSyncInternal(context);
						Logger.i(TAG, "synced");
						AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
					}
					SyncErrorState.getInstance().setSyncError(null);
					BroadcastHelper.sendUpdateAndErrorBroadcast(context);
				} catch (Exception e) {
					Logger.e(TAG, "sync", e);
					AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
					ErrorState syncError = ErrorHelper.getSyncErrorFromException(e, true);
					SyncErrorState.getInstance().setSyncError(syncError);
					BroadcastHelper.sendUpdateAndErrorBroadcast(context);
					throw e;
				}
			}
		}

		private void doSyncInternal(Context context) throws Exception {
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			ApplicationInfo appConfig = appConfigManager.getAppConfig();

			BackendBucketRepository backendBucketRepository =
					new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);
			GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

			if (appConfigManager.getLastSynCallTime() <= currentTime - SYNC_INTERVAL) {
				try {
					Logger.d(TAG, "loading exposees");
					Response<ResponseBody> result =
							backendBucketRepository.getGaenExposees(appConfigManager.getLastKeyBundleTag());

					if (result.code() != 204) {
						File file = new File(context.getCacheDir(),
								KEYFILE_PREFIX + appConfigManager.getLastKeyBundleTag() + ".zip");
						try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
							byte[] bytesIn = new byte[1024];
							int read;
							InputStream bodyStream = result.body().byteStream();
							while ((read = bodyStream.read(bytesIn)) != -1) {
								bos.write(bytesIn, 0, read);
							}
						}

						ArrayList<File> fileList = new ArrayList<>();
						fileList.add(file);
						Logger.d(TAG,
								"provideDiagnosisKeys with size " + file.length());
						appConfigManager.setLastSyncCallTime(currentTime);
						googleExposureClient.provideDiagnosisKeys(fileList);
					} else {
						appConfigManager.setLastSyncCallTime(currentTime);
					}
					appConfigManager.setLastKeyBundleTag(result.headers().get(KEY_BUNDLE_TAG_HEADER));
					appConfigManager.setLastSyncDate(currentTime);
					addHistoryEntry(false, false);
				} catch (Exception e) {
					Logger.e(TAG, "error while syncing new keys", e);
					long lastSuccessfulSyncTime = appConfigManager.getLastSyncDate();
					boolean isDelayWithinGracePeriod =
							lastSuccessfulSyncTime > currentTime - SyncErrorState.getInstance().getSyncErrorGracePeriod();
					if (isDelayWithinGracePeriod && ErrorHelper.isDelayableSyncError(e)) {
						addHistoryEntry(false, true);
					} else {
						addHistoryEntry(true, false);
						throw e;
					}
				}

				cleanupOldKeyFiles(context);
			}
		}

		private void addHistoryEntry(boolean instantError, boolean delayedError) {
			int base = 'A';
			String historyStatus =
					String.valueOf((char) (base + (instantError ? 1 : 0))) + (char) (base + (delayedError ? 1 : 0)) +
							(char) (base + (!instantError && !delayedError ? 1 : 0));
			HistoryDatabase.getInstance(context).addEntry(
					new HistoryEntry(HistoryEntryType.SYNC, historyStatus, !instantError && !delayedError,
							System.currentTimeMillis()));
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

		private void uploadPendingKeys(Context context) {
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			PendingKeyUploadStorage pendingKeyUploadStorage = PendingKeyUploadStorage.getInstance(context);
			PendingKey pendingKey = null;
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
							DP3T.stop(context);
							appConfigManager.setIAmInfectedIsResettable(true);
							continue;
						}
					} else {
						SecureRandom random = new SecureRandom();
						byte[] bytes = new byte[16];
						random.nextBytes(bytes);
						gaenKey = new GaenKey(toBase64(bytes),
								pendingKey.getRollingStartNumber(),
								144,
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
					String status =
							String.valueOf((char) (base + numPendingUploaded)) + (char) (base + numFakePendingUploaded);
					historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.NEXT_DAY_KEY_UPLOAD_REQUEST, status, true,
							System.currentTimeMillis()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (pendingKey != null) {
					pendingKeyUploadStorage.addPendingKey(pendingKey);
					if (appConfigManager.getDevHistory()) {
						HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
						String status = e instanceof StatusCodeException ?
										String.valueOf(((StatusCodeException) e).getCode()) :
										"NETW";
						historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.NEXT_DAY_KEY_UPLOAD_REQUEST, status, false,
								System.currentTimeMillis()));
					}
				}
			}
		}

	}

}
