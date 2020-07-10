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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;

public class ExposureNotificationBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ENBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Logger.i(TAG, "received " + action);

		if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED.equals(action)) {
			ExposureSummary exposureSummary = intent.getParcelableExtra(ExposureNotificationClient.EXTRA_EXPOSURE_SUMMARY);

			if (BuildConfig.FLAVOR.equals("calibration")) {
				Logger.i(TAG, "received update for " + intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN) + " " +
						exposureSummary.toString());
			}

			if (isExposureLimitReached(context, exposureSummary)) {
				Logger.d(TAG, "exposure limit reached");
				DayDate dayOfExposure = new DayDate().subtractDays(exposureSummary.getDaysSinceLastExposure());
				ExposureDay exposureDay = new ExposureDay(-1, dayOfExposure, System.currentTimeMillis());
				ExposureDayStorage.getInstance(context).addExposureDay(context, exposureDay);
			} else {
				Logger.d(TAG, "exposure limit not reached");
			}
		}
	}

	protected boolean isExposureLimitReached(Context context, ExposureSummary exposureSummary) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return computeExposureDuration(appConfigManager, exposureSummary.getAttenuationDurationsInMinutes()) >=
				appConfigManager.getMinDurationForExposure();
	}

	private float computeExposureDuration(AppConfigManager appConfigManager, int[] attenuationDurationsInMinutes) {
		return attenuationDurationsInMinutes[0] * appConfigManager.getAttenuationFactorLow() +
				attenuationDurationsInMinutes[1] * appConfigManager.getAttenuationFactorMedium();
	}

}
