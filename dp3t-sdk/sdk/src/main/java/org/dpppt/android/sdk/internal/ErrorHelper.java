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
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.ApiExceptionUtil;
import org.dpppt.android.sdk.internal.nearby.GaenStateCache;
import org.dpppt.android.sdk.internal.util.LocationServiceUtil;

public class ErrorHelper {

	private static final String TAG = "ErrorHelper";

	private static final Collection<ErrorState> DELAYABLE_SYNC_ERRORS = Arrays.asList(
			ErrorState.SYNC_ERROR_NETWORK,
			ErrorState.SYNC_ERROR_SSLTLS,
			ErrorState.SYNC_ERROR_SERVER,
			ErrorState.SYNC_ERROR_SIGNATURE,
			ErrorState.SYNC_ERROR_API_EXCEPTION
	);

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

		if (!deviceSupportsLocationlessScanning(context) && !LocationServiceUtil.isLocationEnabled(context)) {
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
			errors.add(syncError);
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

	public static boolean deviceSupportsLocationlessScanning(Context context) {
		return Settings.Global.getInt(context.getApplicationContext().getContentResolver(),
				"bluetooth_sanitized_exposure_notification_supported", 0) == 1;
	}

	public static Collection<ErrorState> getDelayableSyncErrors() {
		return new HashSet<>(DELAYABLE_SYNC_ERRORS);
	}

	public static boolean isDelayableSyncError(ErrorState syncError) {
		return DELAYABLE_SYNC_ERRORS.contains(syncError);
	}

	public static boolean isDelayableSyncError(Exception e) {
		return isDelayableSyncError(getSyncErrorFromException(e, false));
	}

	public static ErrorState getSyncErrorFromException(Exception e, boolean setErrorCode) {
		ErrorState syncError;
		if (e instanceof ServerTimeOffsetException) {
			syncError = ErrorState.SYNC_ERROR_TIMING;
		} else if (e instanceof SignatureException) {
			syncError = ErrorState.SYNC_ERROR_SIGNATURE;
		} else if (e instanceof StatusCodeException) {
			syncError = ErrorState.SYNC_ERROR_SERVER;
			if (setErrorCode) syncError.setErrorCode("ASST" + ((StatusCodeException) e).getCode());
		} else if (e instanceof ApiException) {
			ApiException apiException = (ApiException) e;
			int enApiStatusCode = ApiExceptionUtil.getENApiStatusCode(apiException);
			if (enApiStatusCode == ExposureNotificationStatusCodes.FAILED_DISK_IO) {
				syncError = ErrorState.SYNC_ERROR_NO_SPACE;
				if (setErrorCode) syncError.setErrorCode("AGNOSP");
			} else {
				syncError = ErrorState.SYNC_ERROR_API_EXCEPTION;
				if (setErrorCode) syncError.setErrorCode("AGAEN" + apiException.getStatusCode() + "." + enApiStatusCode);
			}
		} else if (e instanceof SSLException) {
			syncError = ErrorState.SYNC_ERROR_SSLTLS;
		} else {
			syncError = ErrorState.SYNC_ERROR_NETWORK;
			if (setErrorCode) syncError.setErrorCode(null);
			if (e instanceof IOException && e.getCause() instanceof ErrnoException) {
				int errorNumber = ((ErrnoException) e.getCause()).errno;
				if (errorNumber == OsConstants.ENOSPC) {
					syncError = ErrorState.SYNC_ERROR_NO_SPACE;
					if (setErrorCode) syncError.setErrorCode("AENOSP");
				}
			}
		}
		return syncError;
	}

}
