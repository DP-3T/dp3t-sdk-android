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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.DP3TCalibrationHelper;

public class ADBBroadcastReceiver extends BroadcastReceiver {

/*
Can be used with the following cmd:
adb shell am broadcast -a org.dpppt.android.calibration.adb --es experimentName "expName" --es deviceName "devName"
 -n org.dpppt.android.calibration/.handshakes.ADBBroadcastReceiver

or

adb shell am broadcast -a org.dpppt.android.calibration.adb --es runMatching "expName"
 -n org.dpppt.android.calibration/.handshakes.ADBBroadcastReceiver
*/

	private static final String EXTRA_SET_DEVICE_NAME = "deviceName";
	private static final String EXTRA_SET_EXPERIMENT_NAME = "experimentName";
	private static final String EXTRA_RUN_MATCHING = "runMatching";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.hasExtra(EXTRA_SET_DEVICE_NAME)) {
			DP3TCalibrationHelper.getInstance(context).setCalibrationTestDeviceName(intent.getStringExtra(EXTRA_SET_DEVICE_NAME));
		}
		if (intent.hasExtra(EXTRA_SET_EXPERIMENT_NAME)) {
			DP3TCalibrationHelper.getInstance(context).setExperimentName(intent.getStringExtra(EXTRA_SET_EXPERIMENT_NAME));
		}
		if (intent.hasExtra(EXTRA_RUN_MATCHING)) {
			MatchingWorker.startMatchingWorker(context, intent.getStringExtra(EXTRA_RUN_MATCHING));
		}
	}

}
