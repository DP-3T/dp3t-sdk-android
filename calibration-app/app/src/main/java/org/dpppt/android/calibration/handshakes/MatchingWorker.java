/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration.handshakes;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.util.HashMap;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.ExposureWindow;

import org.dpppt.android.sdk.internal.logger.Logger;

public class MatchingWorker extends Worker {

	private static final String TAG = "MatchingWorker";
	private static final String ARG_EXPERIMENT_NAME = "argExperimentName";

	public static void startMatchingWorker(Context context, String experimentName) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		WorkRequest myWorkRequest = new OneTimeWorkRequest.Builder(MatchingWorker.class)
				.setInputData(new Data.Builder().putString(ARG_EXPERIMENT_NAME, experimentName).build())
				.setConstraints(constraints)
				.build();
		workManager.enqueue(myWorkRequest);

		Logger.d(TAG, "scheduled SyncWorker");
	}

	public MatchingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public ListenableWorker.Result doWork() {
		Logger.d(TAG, "start MatchingWorker");
		Context context = getApplicationContext();
		try {
			Experiment experiment = HandshakesFragment.getExperiments(context).get(getInputData().getString(ARG_EXPERIMENT_NAME));
			if (experiment == null) {
				Logger.e(TAG, "experiment not found!");
			} else {
				HandshakesFragment.executeAndUploadMatching(context, experiment, new HandshakesFragment.Callback() {
					@Override
					public void onResult(HashMap<String, List<ExposureWindow>> result) {
						Logger.i(TAG, "matching executed and uploaded successfully!");
					}

					@Override
					public void onFailure(Throwable t) {
						Logger.e(TAG, "matching failed!", t);
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ListenableWorker.Result.success();
	}

}
