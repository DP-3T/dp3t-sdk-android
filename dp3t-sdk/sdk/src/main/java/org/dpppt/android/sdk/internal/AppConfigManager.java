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

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.BackendReportRepository;
import org.dpppt.android.sdk.internal.backend.DiscoveryRepository;
import org.dpppt.android.sdk.internal.backend.models.ApplicationsList;
import org.dpppt.android.sdk.internal.util.Json;

public class AppConfigManager {

	private static AppConfigManager instance;

	public static synchronized AppConfigManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppConfigManager(context);
		}
		return instance;
	}

	public static final int CALIBRATION_TEST_DEVICE_NAME_LENGTH = 4;

	private static final String PREFS_NAME = "dp3t_sdk_preferences";
	private static final String PREF_APPLICATION_LIST = "applicationList";
	private static final String PREF_TRACING_ENABLED = "tracingEnabled";
	private static final String PREF_LAST_LOADED_BATCH_RELEASE_TIME = "lastLoadedBatchReleaseTime";
	private static final String PREF_LAST_SYNC_DATE = "lastSyncDate";
	private static final String PREF_LAST_SYNC_NET_SUCCESS = "lastSyncNetSuccess";
	private static final String PREF_I_AM_INFECTED = "IAmInfected";
	private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";

	private String appId;
	private boolean useDiscovery;
	private boolean isDevDiscoveryMode;
	private SharedPreferences sharedPrefs;
	private DiscoveryRepository discoveryRepository;

	private AppConfigManager(Context context) {
		discoveryRepository = new DiscoveryRepository(context);
		sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void triggerLoad() {
		useDiscovery = true;
		discoveryRepository.getDiscovery(new ResponseCallback<ApplicationsList>() {
			@Override
			public void onSuccess(ApplicationsList response) {
				sharedPrefs.edit().putString(PREF_APPLICATION_LIST, Json.toJson(response)).apply();
			}

			@Override
			public void onError(Throwable throwable) {
				throwable.printStackTrace();
			}
		}, isDevDiscoveryMode);
	}

	public void setManualApplicationInfo(ApplicationInfo applicationInfo) {
		useDiscovery = false;
		setAppId(applicationInfo.getAppId());
		ApplicationsList applicationsList = new ApplicationsList();
		applicationsList.getApplications().add(applicationInfo);
		sharedPrefs.edit().putString(PREF_APPLICATION_LIST, Json.toJson(applicationsList)).apply();
	}

	public void updateFromDiscoverySynchronous() throws IOException {
		if (useDiscovery) {
			ApplicationsList response = discoveryRepository.getDiscoverySync(isDevDiscoveryMode);
			sharedPrefs.edit().putString(PREF_APPLICATION_LIST, Json.toJson(response)).apply();
		}
	}

	public ApplicationsList getLoadedApplicationsList() {
		return Json.safeFromJson(sharedPrefs.getString(PREF_APPLICATION_LIST, "{}"), ApplicationsList.class,
				ApplicationsList::new);
	}

	public ApplicationInfo getAppConfig() throws IllegalStateException {
		for (ApplicationInfo application : getLoadedApplicationsList().getApplications()) {
			if (application.getAppId().equals(appId)) {
				return application;
			}
		}
		throw new IllegalStateException("The provided appId is not found by the discovery service!");
	}

	public void setTracingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_TRACING_ENABLED, enabled).apply();
	}

	public boolean isTracingEnabled() {
		return sharedPrefs.getBoolean(PREF_TRACING_ENABLED, false);
	}

	public void setLastLoadedBatchReleaseTime(long lastLoadedBatchReleaseTime) {
		sharedPrefs.edit().putLong(PREF_LAST_LOADED_BATCH_RELEASE_TIME, lastLoadedBatchReleaseTime).apply();
	}

	public long getLastLoadedBatchReleaseTime() {
		return sharedPrefs.getLong(PREF_LAST_LOADED_BATCH_RELEASE_TIME, -1);
	}

	public void setLastSyncDate(long lastSyncDate) {
		sharedPrefs.edit().putLong(PREF_LAST_SYNC_DATE, lastSyncDate).apply();
	}

	public long getLastSyncDate() {
		return sharedPrefs.getLong(PREF_LAST_SYNC_DATE, 0);
	}

	public void setLastSyncNetworkSuccess(boolean success) {
		sharedPrefs.edit().putBoolean(PREF_LAST_SYNC_NET_SUCCESS, success).apply();
	}

	public boolean getLastSyncNetworkSuccess() {
		return sharedPrefs.getBoolean(PREF_LAST_SYNC_NET_SUCCESS, true);
	}

	public boolean getIAmInfected() {
		return sharedPrefs.getBoolean(PREF_I_AM_INFECTED, false);
	}

	public void setIAmInfected(boolean exposed) {
		sharedPrefs.edit().putBoolean(PREF_I_AM_INFECTED, exposed).apply();
	}

	public BackendReportRepository getBackendReportRepository(Context context) throws IllegalStateException {
		ApplicationInfo appConfig = getAppConfig();
		return new BackendReportRepository(context, appConfig.getReportBaseUrl());
	}

	public void setDevDiscoveryModeEnabled(boolean enable) {
		isDevDiscoveryMode = enable;
	}

	public void setCalibrationTestDeviceName(String name) {
		if (name != null && name.length() != CALIBRATION_TEST_DEVICE_NAME_LENGTH) {
			throw new IllegalArgumentException(
					"CalibrationTestDevice Name must have length " + CALIBRATION_TEST_DEVICE_NAME_LENGTH + ", provided string '" +
							name + "' with length " + name.length());
		}
		sharedPrefs.edit().putString(PREF_CALIBRATION_TEST_DEVICE_NAME, name).apply();
	}

	public String getCalibrationTestDeviceName() {
		return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, null);
	}

	public void clearPreferences() {
		sharedPrefs.edit().clear().apply();
	}

}
