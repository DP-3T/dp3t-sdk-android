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

import java.util.concurrent.TimeUnit;

// This DummyWorker is used to be able to start a Worker with
// ExistingPeriodicWorkPolicy.REPLACE to ensure that the job (and all other jobs) is rescheduled
// as a workaround for https://issuetracker.google.com/166292069.
public class DummyWorker extends Worker {

	private static final String WORK_TAG = "DummyWorker";

	public static void startDummyWorker(Context context) {

		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.DAYS)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest);
	}

	public DummyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		return Result.success();
	}

}
