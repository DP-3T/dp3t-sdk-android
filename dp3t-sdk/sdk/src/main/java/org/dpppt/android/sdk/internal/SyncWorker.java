/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.ResponseException;
import org.dpppt.android.sdk.internal.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;

import static org.dpppt.android.sdk.internal.backend.BackendBucketRepository.BATCH_LENGTH;

public class SyncWorker extends Worker {

	private static final String TAG = "SyncWorker";
	private static final String WORK_TAG = "org.dpppt.android.sdk.internal.SyncWorker";

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelAllWorkByTag(WORK_TAG);
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		Context context = getApplicationContext();

		long scanInterval = AppConfigManager.getInstance(getApplicationContext()).getScanInterval();
		TracingService.scheduleNextClientRestart(context, scanInterval);
		TracingService.scheduleNextServerRestart(context);

		try {
			doSync(context);
			Logger.i(TAG, "synced");
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
		} catch (IOException | ResponseException e) {
			Logger.e(TAG, e);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
			return Result.retry();
		}

		return Result.success();
	}

	public static void doSync(Context context) throws IOException, ResponseException {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.updateFromDiscoverySynchronous();
		ApplicationInfo appConfig = appConfigManager.getAppConfig();

		Database database = new Database(context);
		database.generateContactsFromHandshakes(context);

		long lastLoadedBatchReleaseTime = appConfigManager.getLastLoadedBatchReleaseTime();
		long nextBatchReleaseTime;
		if (lastLoadedBatchReleaseTime <= 0 || lastLoadedBatchReleaseTime % BATCH_LENGTH != 0) {
			long now = System.currentTimeMillis();
			nextBatchReleaseTime = now - (now % BATCH_LENGTH);
		} else {
			nextBatchReleaseTime = lastLoadedBatchReleaseTime + BATCH_LENGTH;
		}

		BackendBucketRepository backendBucketRepository =
				new BackendBucketRepository(context, appConfig.getBucketBaseUrl());

		for (long batchReleaseTime = nextBatchReleaseTime;
			 batchReleaseTime < System.currentTimeMillis();
			 batchReleaseTime += BATCH_LENGTH) {

			Exposed.ProtoExposedList result = backendBucketRepository.getExposees(batchReleaseTime);
			long batchReleaseServerTime = result.getBatchReleaseTime();
			for (Exposed.ProtoExposeeOrBuilder exposee : result.getExposedOrBuilderList()) {
				database.addKnownCase(
						context,
						exposee.getKey().toByteArray(),
						exposee.getKeyDate(),
						batchReleaseServerTime
				);
			}

			appConfigManager.setLastLoadedBatchReleaseTime(batchReleaseTime);
		}

		database.removeOldKnownCases();

		appConfigManager.setLastSyncDate(System.currentTimeMillis());

		BroadcastHelper.sendUpdateBroadcast(context);
	}

}
