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
import android.content.Intent;
import androidx.core.util.Consumer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.PackageManagerUtil;

public class GaenStateHelper {

	private static final String TAG = "GaenStateHelper";
	public static boolean SET_GAEN_AVAILABILITY_AVAILABLE_FOR_TESTS = false;

	public static void invalidateGaenAvailability(Context context) {
		checkGaenAvailability(context, null);
	}

	public static void checkGaenAvailability(Context context, Consumer<GaenAvailability> callback) {
		Intent enSettingsIntent = new Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS);
		boolean enModuleAvailable = enSettingsIntent.resolveActivity(context.getPackageManager()) != null;
		if (enModuleAvailable || SET_GAEN_AVAILABILITY_AVAILABLE_FOR_TESTS) {
			Logger.d(TAG, "checkGaenAvailability: EN available");
			publishGaenAvailability(context, callback, GaenAvailability.AVAILABLE);
			return;
		}

		int googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
		if (googlePlayServicesAvailable == ConnectionResult.SERVICE_MISSING ||
				googlePlayServicesAvailable == ConnectionResult.SERVICE_DISABLED ||
				googlePlayServicesAvailable == ConnectionResult.SERVICE_INVALID) {
			Logger.e(TAG, "checkGaenAvailability: googlePlayServicesAvailable=" + googlePlayServicesAvailable);
			publishGaenAvailability(context, callback, GaenAvailability.UNAVAILABLE);
			return;
		} else if (googlePlayServicesAvailable == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
			Logger.w(TAG, "checkGaenAvailability: update required (isGooglePlayServicesAvailable)");
			publishGaenAvailability(context, callback, GaenAvailability.UPDATE_REQUIRED);
			return;
		}

		boolean playServicesInstalled =
				PackageManagerUtil.isPackageInstalled(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, context);
		if (playServicesInstalled) {
			Logger.w(TAG, "checkGaenAvailability: update required (isPackageInstalled)");
			publishGaenAvailability(context, callback, GaenAvailability.UPDATE_REQUIRED);
		} else {
			Logger.w(TAG, "checkGaenAvailability: not installed");
			publishGaenAvailability(context, callback, GaenAvailability.UNAVAILABLE);
		}
	}

	private static void publishGaenAvailability(Context context, Consumer<GaenAvailability> callback,
			GaenAvailability availability) {
		GaenStateCache.setGaenAvailability(availability, context);
		if (callback != null) {
			callback.accept(availability);
		}
	}

	public static void invalidateGaenEnabled(Context context) {
		checkGaenEnabled(context, null);
	}

	public static void checkGaenEnabled(Context context, Consumer<Boolean> callback) {
		if (GaenStateCache.getGaenAvailability() == GaenAvailability.UPDATE_REQUIRED ||
				GaenStateCache.getGaenAvailability() == GaenAvailability.UNAVAILABLE) {
			publishGaenEnabled(context, callback, false, null);
			return;
		}
		GoogleExposureClient.getInstance(context).isEnabled()
				.addOnSuccessListener(enabled -> {
					Logger.d(TAG, "checkGaenEnabled: enabled=" + enabled);
					publishGaenEnabled(context, callback, enabled, null);
				})
				.addOnFailureListener(e -> {
					Logger.e(TAG, "checkGaenEnabled", e);
					publishGaenEnabled(context, callback, false, e);
				});
	}

	private static void publishGaenEnabled(Context context, Consumer<Boolean> callback, boolean enabled, Exception exception) {
		GaenStateCache.setGaenEnabled(enabled, exception, context);
		if (callback != null) {
			callback.accept(enabled);
		}
	}

}
