/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.platformapi;

import android.content.Context;
import androidx.core.util.Consumer;

import org.dpppt.android.sdk.PlatformAPIAvailability;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;

public class PlatformAPIStateHelper {

	public static boolean SET_AVAILABILITY_AVAILABLE_FOR_TESTS = false;

	public static void invalidatePlatformAPIAvailability(Context context) {
		checkPlatformAPIAvailability(context, null);
	}

	public static void checkPlatformAPIAvailability(Context context, Consumer<PlatformAPIAvailability> callback) {
		GaenStateHelper.checkGaenAvailability(context, callback);
	}


	public static void invalidatePlatformAPIEnabled(Context context) {
		checkPlatformAPIEnabled(context, null);
	}

	public static void checkPlatformAPIEnabled(Context context, Consumer<Boolean> callback) {
		GaenStateHelper.checkGaenEnabled(context, callback);
	}

}
