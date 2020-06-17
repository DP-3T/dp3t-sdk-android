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
import androidx.annotation.Nullable;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.internal.BroadcastHelper;

public class GaenStateCache {

	private static GaenAvailability gaenAvailability = null;
	private static Boolean gaenEnabled = null;
	private static Exception apiException = null;

	public static GaenAvailability getGaenAvailability() {
		return gaenAvailability;
	}

	public static void setGaenAvailability(GaenAvailability gaenAvailability, Context context) {
		if (GaenStateCache.gaenAvailability != gaenAvailability) {
			GaenStateCache.gaenAvailability = gaenAvailability;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

	@Nullable
	public static Boolean isGaenEnabled() {
		return gaenEnabled;
	}

	public static Exception getApiException() {
		return apiException;
	}

	public static void setGaenEnabled(boolean gaenEnabled, Exception exception, Context context) {
		GaenStateCache.apiException = exception;
		if (!Boolean.valueOf(gaenEnabled).equals(GaenStateCache.gaenEnabled)) {
			GaenStateCache.gaenEnabled = gaenEnabled;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
