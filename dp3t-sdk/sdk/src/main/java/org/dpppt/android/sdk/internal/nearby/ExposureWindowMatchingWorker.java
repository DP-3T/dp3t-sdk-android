/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.nearby.exposurenotification.ExposureWindow;
import com.google.android.gms.nearby.exposurenotification.ScanInstance;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.platformapi.PlatformAPIWrapper;
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;


public class ExposureWindowMatchingWorker extends Worker {

	private static final String TAG = "MatchingWorker";

	public static void startMatchingWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueue(OneTimeWorkRequest.from(ExposureWindowMatchingWorker.class));

		Logger.d(TAG, "scheduled MatchingWorker");
	}

	public ExposureWindowMatchingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public ListenableWorker.Result doWork() {

		Context context = getApplicationContext();
		List<ExposureWindow> exposureWindows = null;
		try {
			exposureWindows = PlatformAPIWrapper.getInstance(context).getExposureWindows();
		} catch (Exception e) {
			Logger.e(TAG, "error getting exposureWindows");
			return Result.failure();
		}

		addDaysWhereExposureLimitIsReached(context, exposureWindows);

		return Result.success();
	}

	protected static void addDaysWhereExposureLimitIsReached(Context context, List<ExposureWindow> exposureWindows) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		HashMap<DayDate, int[]> attenuationDurationsInSecondsForDate = new HashMap<>();
		for (ExposureWindow exposureWindow : exposureWindows) {

			DayDate windowDate = new DayDate(exposureWindow.getDateMillisSinceEpoch());
			Logger.d(TAG, "Received ExposureWindow for " + windowDate.formatAsString() + ": " + exposureWindow.toString());
			if (!attenuationDurationsInSecondsForDate.containsKey(windowDate)) {
				attenuationDurationsInSecondsForDate.put(windowDate, new int[] { 0, 0, 0 });
			}
			int[] attenuationDurationsInSeconds = attenuationDurationsInSecondsForDate.get(windowDate);

			for (ScanInstance scanInstance : exposureWindow.getScanInstances()) {
				if (scanInstance.getTypicalAttenuationDb() < appConfigManager.getAttenuationThresholdLow()) {
					attenuationDurationsInSeconds[0] += scanInstance.getSecondsSinceLastScan();
				} else if (scanInstance.getTypicalAttenuationDb() < appConfigManager.getAttenuationThresholdMedium()) {
					attenuationDurationsInSeconds[1] += scanInstance.getSecondsSinceLastScan();
				} else {
					attenuationDurationsInSeconds[2] += scanInstance.getSecondsSinceLastScan();
				}
			}
		}

		List<ExposureDay> exposureDays = getExposureDaysFromAttenuationDurations(context, attenuationDurationsInSecondsForDate);
		if (!exposureDays.isEmpty()) {
			ExposureDayStorage.getInstance(context).addExposureDays(context, exposureDays);
		}
	}

	private static List<ExposureDay> getExposureDaysFromAttenuationDurations(Context context,
			HashMap<DayDate, int[]> attenuationDurationsInSecondsForDate) {

		List<ExposureDay> exposureDays = new ArrayList<>();
		DayDate maxAgeForExposure =
				new DayDate().subtractDays(AppConfigManager.getInstance(context).getNumberOfDaysToConsiderForExposure());

		for (Map.Entry<DayDate, int[]> dayDateEntry : attenuationDurationsInSecondsForDate.entrySet()) {

			if (dayDateEntry.getKey().isBefore(maxAgeForExposure)) {
				Logger.d(TAG, "exposure too far in the past on " + dayDateEntry.getKey().formatAsString());
				continue;
			}

			Logger.d(TAG, "Checking exposure limit for " + dayDateEntry.getKey().formatAsString() + ": " + Arrays.toString(dayDateEntry.getValue()));
			int[] attenuationDurationsInMinutes = convertAttenuationDurationsToMinutes(dayDateEntry.getValue());
			if (isExposureLimitReached(context, attenuationDurationsInMinutes)) {
				Logger.d(TAG, "exposure limit reached on " + dayDateEntry.getKey().formatAsString());
				ExposureDay exposureDay = new ExposureDay(-1, dayDateEntry.getKey(), System.currentTimeMillis());
				exposureDays.add(exposureDay);
			} else {
				Logger.d(TAG, "exposure limit not reached on " + dayDateEntry.getKey().formatAsString());
			}
		}
		return exposureDays;
	}

	protected static int[] convertAttenuationDurationsToMinutes(int[] attenuationDurationsInSeconds) {
		int[] attenuationDurationsInMinutes = new int[3];
		for (int i = 0; i < 3; i++) {
			attenuationDurationsInMinutes[i] = (int) Math.ceil(attenuationDurationsInSeconds[i] / 60.0);
		}
		return attenuationDurationsInMinutes;
	}

	protected static boolean isExposureLimitReached(Context context, int[] attenuationDurationsInMinutes) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return computeExposureDuration(appConfigManager, attenuationDurationsInMinutes) >=
				appConfigManager.getMinDurationForExposure();
	}

	private static double computeExposureDuration(AppConfigManager appConfigManager, int[] attenuationDurationsInMinutes) {
		return attenuationDurationsInMinutes[0] * appConfigManager.getAttenuationFactorLow() +
				attenuationDurationsInMinutes[1] * appConfigManager.getAttenuationFactorMedium();
	}

}
