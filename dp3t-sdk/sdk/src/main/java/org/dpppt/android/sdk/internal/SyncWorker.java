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

import java.io.*;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.history.HistoryDatabase;
import org.dpppt.android.sdk.internal.history.HistoryEntry;
import org.dpppt.android.sdk.internal.history.HistoryEntryType;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateCache;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncWorker extends Worker {

	private static final String TAG = "SyncWorker";
	private static final String WORK_NAME = "org.dpppt.android.sdk.internal.SyncWorker";
	private static final String KEYFILE_PREFIX = "keyfile_";

	protected static final String KEY_BUNDLE_TAG_HEADER = "x-key-bundle-tag";

	private static PublicKey bucketSignaturePublicKey;

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 120, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);

		Logger.d(TAG, "scheduled SyncWorker");
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelUniqueWork(WORK_NAME);
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
					if (DP3T.isTracingEnabled(context) && !Boolean.FALSE.equals(GaenStateCache.isGaenEnabled())) {
						boolean syncWasExecuted = doSyncInternal(context);
						if (!syncWasExecuted) {
							Logger.i(TAG, "sync skipped due to rate limit");
							return;
						}
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

		private boolean doSyncInternal(Context context) throws Exception {
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			ApplicationInfo appConfig = appConfigManager.getAppConfig();

			BackendBucketRepository backendBucketRepository =
					new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);
			GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

			if (appConfigManager.getLastSynCallTime() <= currentTime - getSyncInterval()) {
				try {
					Logger.d(TAG, "loading exposees");
					Boolean withFederationGateway = appConfigManager.getWithFederationGateway();
					Response<ResponseBody> result =
							backendBucketRepository.getGaenExposees(appConfigManager.getLastKeyBundleTag(), withFederationGateway);

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
					if (appConfigManager.getDevHistory()) {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						HistoryDatabase.getInstance(context)
								.addEntry(new HistoryEntry(HistoryEntryType.SYNC, sw.toString(), false, currentTime));
					}
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

				return true;
			} else {
				return false;
			}
		}

		private long getSyncInterval() {
			if (BuildConfig.FLAVOR.equals("calibration")) {
				return 5 * 60 * 1000L;
			} else {
				int syncsPerDay = AppConfigManager.getInstance(context).getSyncsPerDay();
				return 24 * 60 * 60 * 1000L / syncsPerDay;
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

	}

}
