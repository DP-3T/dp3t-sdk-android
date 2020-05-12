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

	public enum ErrorState {
		LOCATION_SERVICE_DISABLED(R.string.dp3t_sdk_service_notification_error_location_service),
		BLE_DISABLED(R.string.dp3t_sdk_service_notification_error_bluetooth_disabled),
		BLE_NOT_SUPPORTED(R.string.dp3t_sdk_service_notification_error_bluetooth_not_supported),
		SYNC_ERROR_SERVER(R.string.dp3t_sdk_service_notification_error_sync_server),
		SYNC_ERROR_NETWORK(R.string.dp3t_sdk_service_notification_error_sync_network),
		SYNC_ERROR_DATABASE(R.string.dp3t_sdk_service_notification_error_sync_database),
		SYNC_ERROR_TIMING(R.string.dp3t_sdk_service_notification_error_sync_timing),
		SYNC_ERROR_SIGNATURE(R.string.dp3t_sdk_service_notification_error_sync_signature);

		@StringRes private int errorString;

		ErrorState(@StringRes int errorString) {
			this.errorString = errorString;
		}

		@StringRes
		public int getErrorString() {
			return errorString;
		}
	}

}
