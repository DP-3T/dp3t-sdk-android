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

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.lang.IllegalArgumentException;

import org.dpppt.android.sdk.internal.backend.BackendReportRepository;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;

public class AppConfigManager {

	private static AppConfigManager instance;

	public static synchronized AppConfigManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppConfigManager(context.getApplicationContext());
		}
		return instance;
	}

	private static final String PREFS_NAME = "dp3t_sdk_preferences";
	private static final String PREF_APPLICATION = "application";
	private static final String PREF_TRACING_ENABLED = "tracingEnabled";
	private static final String PREF_LAST_SYNC_DATE = "lastSyncDate";
	private static final String PREF_LAST_SYNC_NET_SUCCESS = "lastSyncNetSuccess";
	private static final String PREF_I_AM_INFECTED = "IAmInfected";
	private static final String PREF_I_AM_INFECTED_IS_RESETTABLE = "IAmInfectedIsResettable";
	private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";
	private static final String PREF_LAST_LOADED_TIMES = "lastLoadedTimes";
	private static final String PREF_LAST_SYNC_CALL_TIMES = "lastExposureClientCalls";
	private static final String PREF_LAST_SUCCESSFUL_SYNC_TIMES = "lastSuccessfulSyncTimes";
	private static final String PREF_DEV_HISTORY = "devHistory";

	private static final String PREF_ATTENUATION_THRESHOLD_LOW = "attenuationThresholdLow";
	private static final String PREF_ATTENUATION_THRESHOLD_MEDIUM = "attenuationThresholdMedium";
	private static final String PREF_ATTENUATION_FACTOR_LOW = "attenuationFactorLow";
	private static final String PREF_ATTENUATION_FACTOR_MEDIUM = "attenuationFactorMedium";
	private static final int DEFAULT_ATTENUATION_THRESHOLD_LOW = 50;
	private static final int DEFAULT_ATTENUATION_THRESHOLD_MEDIUM = 60;
	private static final float DEFAULT_ATTENUATION_FACTOR_LOW = 1.0f;
	private static final float DEFAULT_ATTENUATION_FACTOR_MEDIUM = 0.5f;
	private static final int DEFAULT_MIN_DURATION_FOR_EXPOSURE = 15;
	private static final String PREF_MIN_DURATION_FOR_EXPOSURE = "minDurationForExposure";

	private String appId;
	private SharedPreferences sharedPrefs;
	private GoogleExposureClient googleExposureClient;

	private AppConfigManager(Context context) {
		sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		googleExposureClient = GoogleExposureClient.getInstance(context);
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void setManualApplicationInfo(ApplicationInfo applicationInfo) {
		setAppId(applicationInfo.getAppId());
		sharedPrefs.edit().putString(PREF_APPLICATION, Json.toJson(applicationInfo)).apply();
	}

	public ApplicationInfo getAppConfig() {
		return Json.fromJson(sharedPrefs.getString(PREF_APPLICATION, "{}"), ApplicationInfo.class);
	}

	public void setTracingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_TRACING_ENABLED, enabled).apply();
	}

	public boolean isTracingEnabled() {
		return sharedPrefs.getBoolean(PREF_TRACING_ENABLED, false);
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

	public void setIAmInfected(boolean infected) {
		sharedPrefs.edit().putBoolean(PREF_I_AM_INFECTED, infected).apply();
	}

	public void setIAmInfectedIsResettable(boolean resettable) {
		sharedPrefs.edit().putBoolean(PREF_I_AM_INFECTED_IS_RESETTABLE, resettable).apply();
	}

	public boolean getIAmInfectedIsResettable() {
		return sharedPrefs.getBoolean(PREF_I_AM_INFECTED_IS_RESETTABLE, false);
	}

	public BackendReportRepository getBackendReportRepository(Context context) throws IllegalStateException {
		ApplicationInfo appConfig = getAppConfig();
		return new BackendReportRepository(context, appConfig.getReportBaseUrl());
	}

	public void setCalibrationTestDeviceName(String name) {
		sharedPrefs.edit().putString(PREF_CALIBRATION_TEST_DEVICE_NAME, name).apply();
	}

	public String getCalibrationTestDeviceName() {
		return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, null);
	}

	public void clearPreferences() {
		sharedPrefs.edit().clear().apply();
	}

	public int getMinDurationForExposure() {
		return sharedPrefs.getInt(PREF_MIN_DURATION_FOR_EXPOSURE, DEFAULT_MIN_DURATION_FOR_EXPOSURE);
	}

	public void setMinDurationForExposure(int minDuration) {
		sharedPrefs.edit().putInt(PREF_MIN_DURATION_FOR_EXPOSURE, minDuration).apply();
	}

	public int getAttenuationThresholdLow() {
		return sharedPrefs.getInt(PREF_ATTENUATION_THRESHOLD_LOW, DEFAULT_ATTENUATION_THRESHOLD_LOW);
	}

	public int getAttenuationThresholdMedium() {
		return sharedPrefs.getInt(PREF_ATTENUATION_THRESHOLD_MEDIUM, DEFAULT_ATTENUATION_THRESHOLD_MEDIUM);
	}

	public void setAttenuationThresholds(int thresholdLow, int thresholdMedium) {
		if (thresholdLow >= thresholdMedium) {
			throw new IllegalArgumentException("Illegal Arguments: thresholdLow must be smaller than thresholdMedium");
		}
		sharedPrefs.edit().putInt(PREF_ATTENUATION_THRESHOLD_LOW, thresholdLow).apply();
		sharedPrefs.edit().putInt(PREF_ATTENUATION_THRESHOLD_MEDIUM, thresholdMedium).apply();
		googleExposureClient.setParams(thresholdLow, thresholdMedium);
	}

	public float getAttenuationFactorLow() {
		return sharedPrefs.getFloat(PREF_ATTENUATION_FACTOR_LOW, DEFAULT_ATTENUATION_FACTOR_LOW);
	}

	public void setAttenuationFactorLow(float factor) {
		sharedPrefs.edit().putFloat(PREF_ATTENUATION_FACTOR_LOW, factor).apply();
	}

	public float getAttenuationFactorMedium() {
		return sharedPrefs.getFloat(PREF_ATTENUATION_FACTOR_MEDIUM, DEFAULT_ATTENUATION_FACTOR_MEDIUM);
	}

	public void setAttenuationFactorMedium(float factor) {
		sharedPrefs.edit().putFloat(PREF_ATTENUATION_FACTOR_MEDIUM, factor).apply();
	}

	public HashMap<DayDate, Long> getLastLoadedTimes() {
		return convertToDateMap(Json.fromJson(sharedPrefs.getString(PREF_LAST_LOADED_TIMES, "{}"), StringLongMap.class));
	}

	public HashMap<DayDate, Long> getLastSyncCallTimes() {
		return convertToDateMap(Json.fromJson(sharedPrefs.getString(PREF_LAST_SYNC_CALL_TIMES, "{}"), StringLongMap.class));
	}

	public HashMap<DayDate, Long> getLastSuccessfulSyncTimes() {
		return convertToDateMap(Json.fromJson(sharedPrefs.getString(PREF_LAST_SUCCESSFUL_SYNC_TIMES, "{}"), StringLongMap.class));
	}

	public void setLastLoadedTimes(HashMap<DayDate, Long> lastLoadedTimes) {
		sharedPrefs.edit().putString(PREF_LAST_LOADED_TIMES, Json.toJson(convertFromDateMap(lastLoadedTimes))).apply();
	}

	public void setLastSyncCallTimes(HashMap<DayDate, Long> lastExposureClientCalls) {
		sharedPrefs.edit().putString(PREF_LAST_SYNC_CALL_TIMES, Json.toJson(convertFromDateMap(lastExposureClientCalls))).apply();
	}

	public void setLastSuccessfulSyncTimes(HashMap<DayDate, Long> lastSuccessfulSyncTimes) {
		sharedPrefs.edit().putString(PREF_LAST_SUCCESSFUL_SYNC_TIMES, Json.toJson(convertFromDateMap(lastSuccessfulSyncTimes))).apply();
	}

	public void setDevHistory(boolean devHistory) {
		sharedPrefs.edit().putBoolean(PREF_DEV_HISTORY, devHistory).apply();
	}

	public boolean getDevHistory() {
		return sharedPrefs.getBoolean(PREF_DEV_HISTORY, false);
	}

	private HashMap<DayDate, Long> convertToDateMap(HashMap<String, Long> map) {
		HashMap<DayDate, Long> result = new HashMap<>();
		for (Map.Entry<String, Long> stringLongEntry : map.entrySet()) {
			try {
				result.put(new DayDate(stringLongEntry.getKey()), stringLongEntry.getValue());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private HashMap<String, Long> convertFromDateMap(HashMap<DayDate, Long> map) {
		HashMap<String, Long> result = new HashMap<>();
		for (Map.Entry<DayDate, Long> stringLongEntry : map.entrySet()) {
			result.put(stringLongEntry.getKey().formatAsString(), stringLongEntry.getValue());
		}
		return result;
	}

	private static class StringLongMap extends HashMap<String, Long> { }

}
