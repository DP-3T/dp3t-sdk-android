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

import java.util.Collection;
import java.util.List;

import org.dpppt.android.sdk.internal.database.models.ExposureDay;

public class TracingStatus {

	private int numberOfContacts;
	private boolean advertising;
	private boolean receiving;
	private long lastSyncDate;
	private InfectionStatus infectionStatus;
	private List<ExposureDay> exposureDays;
	private Collection<ErrorState> errors;

	public TracingStatus(int numberOfContacts, boolean advertising, boolean receiving,
			long lastSyncDate,
			InfectionStatus infectionStatus, List<ExposureDay> exposureDays, Collection<ErrorState> errors) {
		this.numberOfContacts = numberOfContacts;
		this.advertising = advertising;
		this.receiving = receiving;
		this.lastSyncDate = lastSyncDate;
		this.infectionStatus = infectionStatus;
		this.exposureDays = exposureDays;
		this.errors = errors;
	}

	public int getNumberOfContacts() {
		return numberOfContacts;
	}

	public boolean isAdvertising() {
		return advertising;
	}

	public boolean isReceiving() {
		return receiving;
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
		BATTERY_OPTIMIZER_ENABLED(R.string.dp3t_sdk_service_notification_error_battery_optimization),
		SYNC_ERROR_SERVER(R.string.dp3t_sdk_service_notification_error_sync_server),
		SYNC_ERROR_NETWORK(R.string.dp3t_sdk_service_notification_error_sync_network),
		SYNC_ERROR_DATABASE(R.string.dp3t_sdk_service_notification_error_sync_database),
		SYNC_ERROR_TIMING(R.string.dp3t_sdk_service_notification_error_sync_timing),
		SYNC_ERROR_SIGNATURE(R.string.dp3t_sdk_service_notification_error_sync_signature);

		private int errorString;

		ErrorState(int errorString) {
			this.errorString = errorString;
		}

		public int getErrorString() {
			return errorString;
		}
	}

}
