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
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.protobuf.InvalidProtocolBufferException;

import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;

import okhttp3.ResponseBody;

import static org.dpppt.android.sdk.internal.backend.BackendBucketRepository.BATCH_LENGTH;

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
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.getMessage());
			return Result.retry();
		}
		Logger.d(TAG, "SyncWorker finished with success");
		return Result.success();
	}

	public static void doSync(Context context)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SQLiteException, SignatureException {
		try {
			doSyncInternal(context);
			Logger.i(TAG, "synced");
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
			SyncErrorState.getInstance().setSyncError(null);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.e(TAG, e);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
			ErrorState syncError;
			if (e instanceof ServerTimeOffsetException) {
				syncError = ErrorState.SYNC_ERROR_TIMING;
			} else if (e instanceof SignatureException) {
				syncError = ErrorState.SYNC_ERROR_SIGNATURE;
			} else if (e instanceof StatusCodeException || e instanceof InvalidProtocolBufferException) {
				syncError = ErrorState.SYNC_ERROR_SERVER;
			} else if (e instanceof SQLiteException) {
				syncError = ErrorState.SYNC_ERROR_DATABASE;
			} else {
				syncError = ErrorState.SYNC_ERROR_NETWORK;
			}
			SyncErrorState.getInstance().setSyncError(syncError);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			throw e;
		}
	}

	private static void doSyncInternal(Context context) throws IOException, StatusCodeException, ServerTimeOffsetException {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		ApplicationInfo appConfig = appConfigManager.getAppConfig();

		long lastLoadedBatchReleaseTime = appConfigManager.getLastLoadedBatchReleaseTime();
		long nextBatchReleaseTime;
		if (lastLoadedBatchReleaseTime <= 0 || lastLoadedBatchReleaseTime % BATCH_LENGTH != 0) {
			long now = System.currentTimeMillis();
			nextBatchReleaseTime = now - (now % BATCH_LENGTH);
		} else {
			nextBatchReleaseTime = lastLoadedBatchReleaseTime + BATCH_LENGTH;
		}

		BackendBucketRepository backendBucketRepository =
				new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

		for (long batchReleaseTime = nextBatchReleaseTime;
			 batchReleaseTime < System.currentTimeMillis();
			 batchReleaseTime += BATCH_LENGTH) {

			ResponseBody result = backendBucketRepository.getGaenExposees(batchReleaseTime);

			ZipInputStream zis = new ZipInputStream(result.byteStream());
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				File file = new File(context.getCacheDir(), batchReleaseTime + "_" + zipEntry.getName());
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				byte[] bytesIn = new byte[1024];
				int read = 0;
				while ((read = zis.read(bytesIn)) != -1) {
					bos.write(bytesIn, 0, read);
				}
				bos.close();
				zis.closeEntry();

				ArrayList<File> fileList = new ArrayList<>();
				fileList.add(file);
				String token = zipEntry.getName();
				googleExposureClient.provideDiagnosisKeys(fileList, token, e -> {
					Logger.e(TAG, e);
					// TODO: add service error state
				});
			}

			appConfigManager.setLastLoadedBatchReleaseTime(batchReleaseTime);
		}

		appConfigManager.setLastSyncDate(System.currentTimeMillis());
	}

}
