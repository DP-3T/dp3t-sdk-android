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

import com.google.android.gms.nearby.exposurenotification.ExposureSummary;

import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;

public class ExposureCheck {

	private static final String TAG = "ExposureCheck";

	public static void handleNewExposureSummary(Context context, ExposureSummary exposureSummary) {

		if (isExposureLimitReached(context, exposureSummary)) {
			Logger.d(TAG, "exposure limit reached");
			DayDate dayOfExposure = new DayDate().subtractDays(exposureSummary.getDaysSinceLastExposure());
			ExposureDay exposureDay = new ExposureDay(-1, dayOfExposure, System.currentTimeMillis());
			ExposureDayStorage.getInstance(context).addExposureDay(context, exposureDay);
		} else {
			Logger.d(TAG, "exposure limit not reached");
		}
	}


	protected static boolean isExposureLimitReached(Context context, ExposureSummary exposureSummary) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return computeExposureDuration(appConfigManager, exposureSummary.getAttenuationDurationsInMinutes()) >=
				appConfigManager.getMinDurationForExposure();
	}

	private static float computeExposureDuration(AppConfigManager appConfigManager, int[] attenuationDurationsInMinutes) {
		return attenuationDurationsInMinutes[0] * appConfigManager.getAttenuationFactorLow() +
				attenuationDurationsInMinutes[1] * appConfigManager.getAttenuationFactorMedium();
	}

}