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

import org.dpppt.android.sdk.internal.backend.BackendRepository;
import org.dpppt.android.sdk.internal.backend.ResponseException;
import org.dpppt.android.sdk.internal.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.models.ExposedList;
import org.dpppt.android.sdk.internal.backend.models.Exposee;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.util.DayDate;

public class SyncWorker extends Worker {

	private static final String TAG = "org.dpppt.android.sdk.internal.SyncWorker";

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelAllWorkByTag(TAG);
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		long scanInterval = AppConfigManager.getInstance(getApplicationContext()).getScanInterval();
		TracingService.scheduleNextRun(getApplicationContext(), scanInterval);

		try {
			doSync(getApplicationContext());
			AppConfigManager.getInstance(getApplicationContext()).setLastSyncNetworkSuccess(true);
		} catch (IOException | ResponseException e) {
			AppConfigManager.getInstance(getApplicationContext()).setLastSyncNetworkSuccess(false);
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

		BackendRepository backendRepository =
				new BackendRepository(context, appConfig.getBackendBaseUrl());

		DayDate dateToLoad = new DayDate();
		dateToLoad = dateToLoad.subtractDays(14);

		for (int i = 0; i <= 14; i++) {

			ExposedList exposedList = backendRepository.getExposees(dateToLoad);
			for (Exposee exposee : exposedList.getExposed()) {
				database.addKnownCase(
						context,
						exposee.getKey(),
						exposee.getOnset(),
						dateToLoad
				);
			}

			dateToLoad = dateToLoad.getNextDay();
		}

		database.removeOldKnownCases();

		appConfigManager.setLastSyncDate(System.currentTimeMillis());
	}

}
