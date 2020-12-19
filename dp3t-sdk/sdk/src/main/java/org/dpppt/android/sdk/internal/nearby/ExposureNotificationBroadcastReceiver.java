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

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.internal.logger.Logger;

public class ExposureNotificationBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ENBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Logger.i(TAG, "received " + action);

		if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED.equals(action)) {

			if (BuildConfig.FLAVOR.equals("calibration")) {
				Logger.i(TAG, "received update for " + intent.toString());
			}

			ExposureWindowMatchingWorker.startMatchingWorker(context);
		}
	}

}
