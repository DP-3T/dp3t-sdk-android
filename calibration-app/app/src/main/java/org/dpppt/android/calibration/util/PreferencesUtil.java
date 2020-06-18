/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtil {

	private static final String PREFS_DP3T_SDK_SAMPLE = "preferences_dp3t_sdk_sample";
	private static final String PREF_KEY_EXPOSED_NOTIFICATION = "pref_key_exposed_notification";

	public static boolean isExposedNotificationShown(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_DP3T_SDK_SAMPLE, Context.MODE_PRIVATE);
		return prefs.getBoolean(PREF_KEY_EXPOSED_NOTIFICATION, false);
	}

	public static void setExposedNotificationShown(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_DP3T_SDK_SAMPLE, Context.MODE_PRIVATE);
		prefs.edit().putBoolean(PREF_KEY_EXPOSED_NOTIFICATION, true).apply();
	}

}
