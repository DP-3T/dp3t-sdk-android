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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.android.gms.common.api.ApiException;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateCache;
import org.dpppt.android.sdk.internal.util.LocationServiceUtil;

public class ErrorHelper {

	private static final String TAG = "ErrorHelper";

	public static Collection<ErrorState> checkTracingErrorStatus(Context context, AppConfigManager appConfigManager) {
		Set<ErrorState> errors = new HashSet<>();

		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			errors.add(ErrorState.BLE_NOT_SUPPORTED);
		} else {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				errors.add(ErrorState.BLE_DISABLED);
			}
		}

		if (!LocationServiceUtil.isLocationEnabled(context)) {
			errors.add(ErrorState.LOCATION_SERVICE_DISABLED);
		}

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		boolean batteryOptimizationsDeactivated =
				powerManager == null || powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
		if (!batteryOptimizationsDeactivated) {
			errors.add(ErrorState.BATTERY_OPTIMIZER_ENABLED);
		}

		if (!AppConfigManager.getInstance(context).getLastSyncNetworkSuccess()) {
			SyncErrorState syncErrorState = SyncErrorState.getInstance();
			ErrorState syncError = syncErrorState.getSyncError();
			if (syncError == null) {
				Logger.w(TAG, "lost sync error state");
				syncError = ErrorState.SYNC_ERROR_NETWORK;
				syncError.setErrorCode("LOST");
			}
			boolean isSuppressibleError =
					syncError == ErrorState.SYNC_ERROR_NETWORK || syncError == ErrorState.SYNC_ERROR_SSLTLS ||
							(syncError == ErrorState.SYNC_ERROR_SERVER &&
									("ASST502".equals(syncError.getErrorCode()) || "ASST503".equals(syncError.getErrorCode())));
			boolean showSuppressibleError = appConfigManager.isTracingEnabled() &&
					appConfigManager.getLastSyncDate() < System.currentTimeMillis() - syncErrorState.getNetworkErrorGracePeriod();
			if (!isSuppressibleError || showSuppressibleError) {
				errors.add(syncError);
			}
		}

		GaenAvailability gaenAvailability = GaenStateCache.getGaenAvailability();
		if (gaenAvailability != null && gaenAvailability != GaenAvailability.AVAILABLE) {
			errors.add(ErrorState.GAEN_NOT_AVAILABLE);
		}

		if (appConfigManager.isTracingEnabled() && Boolean.FALSE.equals(GaenStateCache.isGaenEnabled())) {
			ErrorState errorState = ErrorState.GAEN_UNEXPECTEDLY_DISABLED;
			Exception exception = GaenStateCache.getApiException();
			if (exception == null) {
				errorState.setErrorCode("GAUD-00");
			} else if (exception instanceof ApiException) {
				errorState.setErrorCode("GAUD-" + ((ApiException) exception).getStatusCode());
			} else {
				errorState.setErrorCode("GAUD-" + exception.getMessage());
			}
			errors.add(errorState);
		}

		return errors;
	}

}
