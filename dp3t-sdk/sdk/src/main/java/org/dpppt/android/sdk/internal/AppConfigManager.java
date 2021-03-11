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

import androidx.annotation.Nullable;

import org.dpppt.android.sdk.internal.backend.BackendReportRepository;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.ApplicationInfo;

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
    private static final String PREF_NUMBER_OF_SYNCS_PER_DAY = "numberOfSyncsPerDay";
    public static final int MAX_SYNCS_PER_DAY = 6;
    public static final int MIN_SYNCS_PER_DAY = 1;
    private static final String PREF_LAST_SYNC_NET_SUCCESS = "lastSyncNetSuccess";
	private static final String PREF_I_AM_INFECTED = "IAmInfected";
	private static final String PREF_I_AM_INFECTED_IS_RESETTABLE = "IAmInfectedIsResettable";
	private static final String PREF_LAST_SYNC_CALL_TIME = "lastSyncCallTime";
	private static final String PREF_LAST_KEY_BUNDLE_TAG = "lastKeyBundleTag";
	private static final String PREF_DEV_HISTORY = "devHistory";
	private static final String PREF_EN_MODULE_VERSION = "enModuleVersion";
	private static final String PREF_NUMBER_OF_DAYS_TO_CONSIDER_FOR_EXPOSURE = "numberOfDaysToConsiderForExposure";
	private static final String PREF_NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS = "numberOfDaysToKeepExposedDays";
	private static final String PREF_WITH_FEDERATION_GATEWAY = "withFederationGateway";
	private static final String PREF_APP_VERSION_CODE = "appVersionCode";

	private static final String PREF_ATTENUATION_THRESHOLD_LOW = "attenuationThresholdLow";
	private static final String PREF_ATTENUATION_THRESHOLD_MEDIUM = "attenuationThresholdMedium";
	private static final String PREF_ATTENUATION_FACTOR_LOW = "attenuationFactorLow";
	private static final String PREF_ATTENUATION_FACTOR_MEDIUM = "attenuationFactorMedium";
	private static final int DEFAULT_ATTENUATION_THRESHOLD_LOW = 55;
	private static final int DEFAULT_ATTENUATION_THRESHOLD_MEDIUM = 63;
	private static final float DEFAULT_ATTENUATION_FACTOR_LOW = 1.0f;
	private static final float DEFAULT_ATTENUATION_FACTOR_MEDIUM = 0.5f;
	private static final int DEFAULT_MIN_DURATION_FOR_EXPOSURE = 15;
	private static final String PREF_MIN_DURATION_FOR_EXPOSURE = "minDurationForExposure";
	public static final int DEFAULT_NUMBER_OF_DAYS_TO_CONSIDER_FOR_EXPOSURE = 10;
	public static final int DEFAULT_NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS = 14;

	private SharedPreferences sharedPrefs;

	private AppConfigManager(Context context) {
		sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	public void setManualApplicationInfo(ApplicationInfo applicationInfo) {
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

    public void setSyncsPerDay(int syncsPerDay) {
	    if (syncsPerDay > MAX_SYNCS_PER_DAY) {
            syncsPerDay = MAX_SYNCS_PER_DAY;
        } else if (syncsPerDay < MIN_SYNCS_PER_DAY) {
            syncsPerDay = MIN_SYNCS_PER_DAY;
        }
        sharedPrefs.edit().putInt(PREF_NUMBER_OF_SYNCS_PER_DAY, syncsPerDay).apply();
    }

    public int getSyncsPerDay() {
        return sharedPrefs.getInt(PREF_NUMBER_OF_SYNCS_PER_DAY, MAX_SYNCS_PER_DAY);
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

	public int getNumberOfDaysToConsiderForExposure() {
		return sharedPrefs.getInt(PREF_NUMBER_OF_DAYS_TO_CONSIDER_FOR_EXPOSURE, DEFAULT_NUMBER_OF_DAYS_TO_CONSIDER_FOR_EXPOSURE);
	}

	public void setNumberOfDaysToConsiderForExposure(int days) {
		sharedPrefs.edit().putInt(PREF_NUMBER_OF_DAYS_TO_CONSIDER_FOR_EXPOSURE, days).apply();
	}

	public int getNumberOfDaysToKeepExposedDays() {
		return sharedPrefs.getInt(PREF_NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS, DEFAULT_NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS);
	}

	public void setNumberOfDaysToKeepExposedDays(int days) {
		sharedPrefs.edit().putInt(PREF_NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS, days).apply();
	}

	public long getLastSynCallTime() {
		return sharedPrefs.getLong(PREF_LAST_SYNC_CALL_TIME, 0);
	}

	public void setLastSyncCallTime(long time) {
		sharedPrefs.edit().putLong(PREF_LAST_SYNC_CALL_TIME, time).apply();
	}

	public String getLastKeyBundleTag() {
		return sharedPrefs.getString(PREF_LAST_KEY_BUNDLE_TAG, null);
	}

	public void setLastKeyBundleTag(String tag) {
		sharedPrefs.edit().putString(PREF_LAST_KEY_BUNDLE_TAG, tag).apply();
	}

	public void setDevHistory(boolean devHistory) {
		sharedPrefs.edit().putBoolean(PREF_DEV_HISTORY, devHistory).apply();
	}

	public boolean getDevHistory() {
		return sharedPrefs.getBoolean(PREF_DEV_HISTORY, false);
	}

	public void setENModuleVersion(long version) {
		sharedPrefs.edit().putLong(PREF_EN_MODULE_VERSION, version).apply();
	}

	public long getENModuleVersion() {
		return sharedPrefs.getLong(PREF_EN_MODULE_VERSION, 0);
	}

	public void setWithFederationGateway(@Nullable Boolean withFederationGateway) {
		if (withFederationGateway == null) {
			sharedPrefs.edit().remove(PREF_WITH_FEDERATION_GATEWAY).apply();
		} else {
			sharedPrefs.edit().putBoolean(PREF_WITH_FEDERATION_GATEWAY, withFederationGateway).apply();
		}
	}

	public Boolean getWithFederationGateway() {
		return sharedPrefs.contains(PREF_WITH_FEDERATION_GATEWAY)
			   ? sharedPrefs.getBoolean(PREF_WITH_FEDERATION_GATEWAY, false)
			   : null;
	}

	public void setLastKnownAppVersionCode(int versionCode) {
		sharedPrefs.edit().putInt(PREF_APP_VERSION_CODE, versionCode).apply();
	}

	public int getLastKnownAppVersionCode() {
		return sharedPrefs.getInt(PREF_APP_VERSION_CODE, -1);
	}

}
