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
import android.content.pm.PackageManager;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.util.Consumer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;

import org.dpppt.android.sdk.GaenAvailability;

public class GaenAvailabilityHelper {

	private static final int PLAY_SERVICES_MIN_VERSION = 201813000;

	@Nullable
	public static void checkGaenAvailability(Context context, Consumer<GaenAvailability> availabilityCallback) {
		int googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
		if (googlePlayServicesAvailable == ConnectionResult.SERVICE_MISSING ||
				googlePlayServicesAvailable == ConnectionResult.SERVICE_DISABLED ||
				googlePlayServicesAvailable == ConnectionResult.SERVICE_INVALID) {
			availabilityCallback.accept(GaenAvailability.UNAVAILABLE);
			return;
		} else if (googlePlayServicesAvailable == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ) {
			availabilityCallback.accept(GaenAvailability.UPDATE_REQUIRED);
			return;
		}

		try {
			long installedPlayServicesVersion = PackageInfoCompat.getLongVersionCode(
					context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0));
			if (installedPlayServicesVersion < PLAY_SERVICES_MIN_VERSION) {
				availabilityCallback.accept(GaenAvailability.UPDATE_REQUIRED);
				return;
			}
		} catch (PackageManager.NameNotFoundException ignored) {
			availabilityCallback.accept(GaenAvailability.UNAVAILABLE);
			return;
		}

		GoogleExposureClient.getInstance(context).isEnabled()
				.addOnSuccessListener(v -> availabilityCallback.accept(GaenAvailability.AVAILABLE))
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						int statusCode = ((ApiException) e).getStatusCode();
						if (statusCode == 17) {
							availabilityCallback.accept(GaenAvailability.UPDATE_REQUIRED);
							return;
						}
					}
					availabilityCallback.accept(GaenAvailability.UNAVAILABLE);
				});
	}

}
