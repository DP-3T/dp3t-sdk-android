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
import androidx.annotation.Nullable;

import org.dpppt.android.sdk.PlatformAPIAvailability;
import org.dpppt.android.sdk.internal.BroadcastHelper;

public class PlatformAPIStateCache {

	private static PlatformAPIAvailability platformAPIAvailability = null;
	private static Boolean platformAPIEnabled = null;
	private static Exception apiException = null;

	public static PlatformAPIAvailability getPlatformAPIAvailability() {
		return platformAPIAvailability;
	}

	public static void setPlatformAPIAvailability(PlatformAPIAvailability platformAPIAvailability, Context context) {
		if (PlatformAPIStateCache.platformAPIAvailability != platformAPIAvailability) {
			PlatformAPIStateCache.platformAPIAvailability = platformAPIAvailability;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

	@Nullable
	public static Boolean isPlatformAPIEnabled() {
		return platformAPIEnabled;
	}

	public static Exception getApiException() {
		return apiException;
	}

	public static void setPlatformAPIEnabled(boolean platformAPIEnabled, Exception exception, Context context) {
		PlatformAPIStateCache.apiException = exception;
		if (!Boolean.valueOf(platformAPIEnabled).equals(PlatformAPIStateCache.platformAPIEnabled)) {
			PlatformAPIStateCache.platformAPIEnabled = platformAPIEnabled;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
