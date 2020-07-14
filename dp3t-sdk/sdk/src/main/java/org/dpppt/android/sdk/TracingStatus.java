/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk;

import android.content.Context;
import androidx.annotation.Keep;
import androidx.annotation.StringRes;

import java.util.Collection;
import java.util.List;

import org.dpppt.android.sdk.models.ExposureDay;

public class TracingStatus {

	private boolean tracingEnabled;
	private long lastSyncDate;
	private InfectionStatus infectionStatus;
	private List<ExposureDay> exposureDays;
	private Collection<ErrorState> errors;

	public TracingStatus(boolean tracingEnabled, long lastSyncDate,
			InfectionStatus infectionStatus, List<ExposureDay> exposureDays, Collection<ErrorState> errors) {
		this.tracingEnabled = tracingEnabled;
		this.lastSyncDate = lastSyncDate;
		this.infectionStatus = infectionStatus;
		this.exposureDays = exposureDays;
		this.errors = errors;
	}

	public boolean isTracingEnabled() {
		return tracingEnabled;
	}

	public long getLastSyncDate() {
		return lastSyncDate;
	}

	public InfectionStatus getInfectionStatus() {
		return infectionStatus;
	}

	public List<ExposureDay> getExposureDays() {
		return exposureDays;
	}

	public Collection<ErrorState> getErrors() {
		return errors;
	}

	@Keep
	public enum ErrorState {
		LOCATION_SERVICE_DISABLED(R.string.dp3t_sdk_service_notification_error_location_service),
		BLE_DISABLED(R.string.dp3t_sdk_service_notification_error_bluetooth_disabled),
		BLE_NOT_SUPPORTED(R.string.dp3t_sdk_service_notification_error_bluetooth_not_supported),
		GAEN_NOT_AVAILABLE(R.string.dp3t_sdk_service_notification_error_gaen_not_available),
		GAEN_UNEXPECTEDLY_DISABLED(R.string.dp3t_sdk_service_notification_error_gaen_unexpectedly_disabled),
		BATTERY_OPTIMIZER_ENABLED(R.string.dp3t_sdk_service_notification_error_battery_optimization),
		SYNC_ERROR_SERVER(R.string.dp3t_sdk_service_notification_error_sync_server),
		SYNC_ERROR_NETWORK(R.string.dp3t_sdk_service_notification_error_sync_network),
		SYNC_ERROR_NO_SPACE(R.string.dp3t_sdk_service_notification_error_no_space),
		SYNC_ERROR_SSLTLS(R.string.dp3t_sdk_service_notification_error_sync_ssltls),
		SYNC_ERROR_TIMING(R.string.dp3t_sdk_service_notification_error_sync_timing),
		SYNC_ERROR_SIGNATURE(R.string.dp3t_sdk_service_notification_error_sync_signature),
		SYNC_ERROR_API_EXCEPTION(R.string.dp3t_sdk_service_notification_error_sync_api);

		@StringRes private int errorString;
		private String errorCode;

		ErrorState(@StringRes int errorString) {
			this.errorString = errorString;
		}

		@SuppressWarnings("java:S3066")
		//it is ok in our case to set the errorCode to the always latest value, it is only used as debug information
		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}

		public String getErrorCode() {
			return errorCode != null ? errorCode : "";
		}

		public String getErrorString(Context context) {
			if (errorString == -1) {
				return null;
			}
			String text = context.getString(errorString);
			if (errorCode != null) {
				text += " (" + errorCode + ")";
			}
			return text;
		}

		public static ErrorState tryValueOf(String name) {
			for (ErrorState value : values()) {
				if (value.name().equals(name)) {
					return value;
				}
			}
			return null;
		}
	}

}
