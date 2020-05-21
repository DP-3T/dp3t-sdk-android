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

	public static class ErrorState {
		public static ErrorState LOCATION_SERVICE_DISABLED =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_location_service);
		public static ErrorState BLE_DISABLED = new ErrorState(R.string.dp3t_sdk_service_notification_error_bluetooth_disabled);
		public static ErrorState BLE_NOT_SUPPORTED =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_bluetooth_not_supported);
		public static ErrorState GAEN_NOT_AVAILABLE =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_gaen_not_available);
		public static ErrorState GAEN_UNEXPECTEDLY_DISABLED =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_gaen_unexpectedly_disabled);
		public static ErrorState BATTERY_OPTIMIZER_ENABLED =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_battery_optimization);
		public static ErrorState SYNC_ERROR_SERVER = new ErrorState(R.string.dp3t_sdk_service_notification_error_sync_server);
		public static ErrorState SYNC_ERROR_NETWORK = new ErrorState(R.string.dp3t_sdk_service_notification_error_sync_network);
		public static ErrorState SYNC_ERROR_DATABASE = new ErrorState(R.string.dp3t_sdk_service_notification_error_sync_database);
		public static ErrorState SYNC_ERROR_TIMING = new ErrorState(R.string.dp3t_sdk_service_notification_error_sync_timing);
		public static ErrorState SYNC_ERROR_SIGNATURE =
				new ErrorState(R.string.dp3t_sdk_service_notification_error_sync_signature);

		@StringRes private int errorStringRes;
		private String errorString;

		private ErrorState(@StringRes int errorStringRes) {
			this(errorStringRes, null);
		}

		public ErrorState(@StringRes int errorStringRes, String errorString) {
			this.errorStringRes = errorStringRes;
			this.errorString = errorString;
		}

		public String getErrorString(Context context) {
			String text = context.getString(errorStringRes);
			if (errorString != null) {
				text += errorString;
			}
			return text;
		}

	}

}
