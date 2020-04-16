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

import com.google.gson.Gson;

import org.dpppt.android.sdk.internal.backend.BackendRepository;
import org.dpppt.android.sdk.internal.backend.CallbackListener;
import org.dpppt.android.sdk.internal.backend.DiscoveryRepository;
import org.dpppt.android.sdk.internal.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.models.ApplicationsList;

public class AppConfigManager {

	private static AppConfigManager instance;

	public static synchronized AppConfigManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppConfigManager(context);
		}
		return instance;
	}

	public static final int CALIBRATION_TEST_DEVICE_NAME_LENGTH = 4;

	public static final long DEFAULT_SCAN_INTERVAL = 5 * 60 * 1000L;
	private static final long DEFAULT_SCAN_DURATION = 30 * 1000L;
	private static final int DEFAULT_BLUETOOTH_POWER_LEVEL = BluetoothTxPowerLevel.ADVERTISE_TX_POWER_LOW.getValue();
	private static final int DEFAULT_BLUETOOTH_ADVERTISE_MODE = BluetoothAdvertiseMode.ADVERTISE_MODE_LOW_POWER.getValue();

	private static final String PREF_NAME = "appConfigPreferences";
	private static final String PREF_APPLICATION_LIST = "applicationList";
	private static final String PREF_ADVERTISING_ENABLED = "advertisingEnabled";
	private static final String PREF_RECEIVING_ENABLED = "receivingEnabled";
	private static final String PREF_LAST_SYNC_DATE = "lastSyncDate";
	private static final String PREF_LAST_SYNC_NET_SUCCESS = "lastSyncNetSuccess";
	private static final String PREF_AM_I_EXPOSED = "amIExposed";
	private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";
	private static final String PREF_SCAN_INTERVAL = "scanInterval";
	private static final String PREF_SCAN_DURATION = "scanDuration";
	private static final String PREF_ADVERTISEMENT_POWER_LEVEL = "advertisementPowerLevel";
	private static final String PREF_ADVERTISEMENT_MODE = "advertisementMode";

	private String appId;
	private boolean useDiscovery;
	private boolean isDevDiscoveryMode;
	private SharedPreferences sharedPrefs;
	private DiscoveryRepository discoveryRepository;

	private AppConfigManager(Context context) {
		discoveryRepository = new DiscoveryRepository(context);
		sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void triggerLoad() {
		useDiscovery = true;
		discoveryRepository.getDiscovery(new CallbackListener<ApplicationsList>() {
			@Override
			public void onSuccess(ApplicationsList response) {
				sharedPrefs.edit().putString(PREF_APPLICATION_LIST, new Gson().toJson(response)).commit();
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
		sharedPrefs.edit().putString(PREF_APPLICATION_LIST, new Gson().toJson(applicationsList)).commit();
	}

	public void updateFromDiscoverySynchronous() throws IOException {
		if (useDiscovery) {
			ApplicationsList response = discoveryRepository.getDiscoverySync(isDevDiscoveryMode);
			sharedPrefs.edit().putString(PREF_APPLICATION_LIST, new Gson().toJson(response)).commit();
		}
	}

	public ApplicationsList getLoadedApplicationsList() {
		return new Gson().fromJson(sharedPrefs.getString(PREF_APPLICATION_LIST, "{}"), ApplicationsList.class);
	}

	public ApplicationInfo getAppConfig() {
		for (ApplicationInfo application : getLoadedApplicationsList().getApplications()) {
			if (application.getAppId().equals(appId)) {
				return application;
			}
		}
		throw new IllegalStateException("The provided appId is not found by the discovery service!");
	}

	public void setAdvertisingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_ADVERTISING_ENABLED, enabled).commit();
	}

	public boolean isAdvertisingEnabled() {
		return sharedPrefs.getBoolean(PREF_ADVERTISING_ENABLED, false);
	}

	public void setReceivingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_RECEIVING_ENABLED, enabled).commit();
	}

	public boolean isReceivingEnabled() {
		return sharedPrefs.getBoolean(PREF_RECEIVING_ENABLED, false);
	}

	public void setLastSyncDate(long lastSyncDate) {
		sharedPrefs.edit().putLong(PREF_LAST_SYNC_DATE, lastSyncDate).commit();
	}

	public long getLastSyncDate() {
		return sharedPrefs.getLong(PREF_LAST_SYNC_DATE, 0);
	}

	public void setLastSyncNetworkSuccess(boolean success) {
		sharedPrefs.edit().putBoolean(PREF_LAST_SYNC_NET_SUCCESS, success).commit();
	}

	public boolean getLastSyncNetworkSuccess() {
		return sharedPrefs.getBoolean(PREF_LAST_SYNC_NET_SUCCESS, true);
	}

	public boolean getAmIExposed() {
		return sharedPrefs.getBoolean(PREF_AM_I_EXPOSED, false);
	}

	public void setAmIExposed(boolean exposed) {
		sharedPrefs.edit().putBoolean(PREF_AM_I_EXPOSED, exposed).commit();
	}

	public BackendRepository getBackendRepository(Context context) {
		ApplicationInfo appConfig = getAppConfig();
		//TODO what if appConfig is not yet loaded?
		return new BackendRepository(context, appConfig.getBackendBaseUrl());
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
		sharedPrefs.edit().putString(PREF_CALIBRATION_TEST_DEVICE_NAME, name).commit();
	}

	public String getCalibrationTestDeviceName() {
		return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, null);
	}

	public void setScanDuration(long scanDuration) {
		sharedPrefs.edit().putLong(PREF_SCAN_DURATION, scanDuration).commit();
	}

	public long getScanDuration() {
		return sharedPrefs.getLong(PREF_SCAN_DURATION, DEFAULT_SCAN_DURATION);
	}

	public void setScanInterval(long scanInterval) {
		sharedPrefs.edit().putLong(PREF_SCAN_INTERVAL, scanInterval).commit();
	}

	public long getScanInterval() {
		return sharedPrefs.getLong(PREF_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL);
	}

	public void setBluetoothPowerLevel(BluetoothTxPowerLevel powerLevel) {
		sharedPrefs.edit().putInt(PREF_ADVERTISEMENT_POWER_LEVEL, powerLevel.ordinal()).commit();
	}

	public BluetoothTxPowerLevel getBluetoothTxPowerLevel() {
		return BluetoothTxPowerLevel.values()[sharedPrefs.getInt(PREF_ADVERTISEMENT_POWER_LEVEL, DEFAULT_BLUETOOTH_POWER_LEVEL)];
	}

	public void setBluetoothAdvertiseMode(BluetoothAdvertiseMode advertiseMode) {
		sharedPrefs.edit().putInt(PREF_ADVERTISEMENT_MODE, advertiseMode.ordinal()).commit();
	}

	public BluetoothAdvertiseMode getBluetoothAdvertiseMode() {
		return BluetoothAdvertiseMode.values()[sharedPrefs.getInt(PREF_ADVERTISEMENT_MODE, DEFAULT_BLUETOOTH_ADVERTISE_MODE)];
	}

	public void clearPreferences() {
		sharedPrefs.edit().clear().commit();
	}

}
