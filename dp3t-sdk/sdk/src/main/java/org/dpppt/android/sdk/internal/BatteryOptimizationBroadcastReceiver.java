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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.internal.logger.Logger;

public class BatteryOptimizationBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "BatteryOptimizationBR";

	public static final String ACTION_POWER_SAVE_WHITELIST_CHANGED = "android.os.action.POWER_SAVE_WHITELIST_CHANGED";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!ACTION_POWER_SAVE_WHITELIST_CHANGED.equals(intent.getAction()))
			return;

		Logger.w(TAG, intent.getAction());
		BroadcastHelper.sendUpdateAndErrorBroadcast(context);
	}

}
