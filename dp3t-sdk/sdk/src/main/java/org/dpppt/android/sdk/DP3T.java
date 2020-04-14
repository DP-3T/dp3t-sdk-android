/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.SyncWorker;
import org.dpppt.android.sdk.internal.TracingService;
import org.dpppt.android.sdk.internal.backend.CallbackListener;
import org.dpppt.android.sdk.internal.backend.ResponseException;
import org.dpppt.android.sdk.internal.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.models.ExposeeAuthData;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.dpppt.android.sdk.internal.util.ProcessUtil;

public class DP3T {

	public static final String UPDATE_INTENT_ACTION = "org.dpppt.android.sdk.UPDATE_ACTION";

	private static String appId;

	public static void init(Context context, String appId) {
		init(context, appId, false);
	}

	public static void init(Context context, String appId, boolean enableDevDiscoveryMode) {
		if (ProcessUtil.isMainProcess(context)) {
			DP3T.appId = appId;
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			appConfigManager.setAppId(appId);
			appConfigManager.setDevDiscoveryModeEnabled(enableDevDiscoveryMode);
			appConfigManager.triggerLoad();

			executeInit(context);
		}
	}

	public static void init(Context context, ApplicationInfo applicationInfo) {
		if (ProcessUtil.isMainProcess(context)) {
			DP3T.appId = applicationInfo.getAppId();
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			appConfigManager.setManualApplicationInfo(applicationInfo);

			executeInit(context);
		}
	}

	private static void executeInit(Context context) {
		CryptoModule.getInstance(context).init();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		boolean advertising = appConfigManager.isAdvertisingEnabled();
		boolean receiving = appConfigManager.isReceivingEnabled();
		if (advertising || receiving) {
			start(context, advertising, receiving);
		}
	}

	private static void checkInit() {
		if (appId == null) {
			throw new IllegalStateException("You have to call STARTracing.init() in your application onCreate()");
		}
	}

	public static void start(Context context) {
		start(context, true, true);
	}

	public static void start(Context context, boolean advertise, boolean receive) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(advertise);
		appConfigManager.setReceivingEnabled(receive);
		long scanInterval = appConfigManager.getScanInterval();
		long scanDuration = appConfigManager.getScanDuration();
		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_START);
		intent.putExtra(TracingService.EXTRA_ADVERTISE, advertise);
		intent.putExtra(TracingService.EXTRA_RECEIVE, receive);
		intent.putExtra(TracingService.EXTRA_SCAN_INTERVAL, scanInterval);
		intent.putExtra(TracingService.EXTRA_SCAN_DURATION, scanDuration);
		ContextCompat.startForegroundService(context, intent);
		SyncWorker.startSyncWorker(context);
	}

	public static boolean isStarted(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled();
	}

	public static void sync(Context context) {
		checkInit();
		try {
			SyncWorker.doSync(context);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
		} catch (IOException | ResponseException e) {
			e.printStackTrace();
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
		}
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		Database database = new Database(context);
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		ArrayList<TracingStatus.ErrorState> errorStates = checkTracingStatus(context);
		return new TracingStatus(
				database.getContacts().size(),
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				database.wasContactExposed(),
				appConfigManager.getLastSyncDate(),
				appConfigManager.getAmIExposed(),
				errorStates
		);
	}

	private static ArrayList<TracingStatus.ErrorState> checkTracingStatus(Context context) {
		ArrayList<TracingStatus.ErrorState> errors = new ArrayList<>();

		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			errors.add(TracingStatus.ErrorState.BLE_DISABLED);
		}

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		boolean batteryOptimizationsDeactivated = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
		if (!batteryOptimizationsDeactivated) {
			errors.add(TracingStatus.ErrorState.BATTERY_OPTIMIZER_ENABLED);
		}

		boolean locationPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
				PackageManager.PERMISSION_GRANTED;
		if (!locationPermissionGranted) {
			errors.add(TracingStatus.ErrorState.MISSING_LOCATION_PERMISSION);
		}

		if (!AppConfigManager.getInstance(context).getLastSyncNetworkSuccess()) {
			errors.add(TracingStatus.ErrorState.NETWORK_ERROR_WHILE_SYNCING);
		}

		return errors;
	}

	public static void sendIWasExposed(Context context, Date onset, ExposeeAuthData exposeeAuthData,
			CallbackListener<Void> callback) {
		checkInit();
		DayDate onsetDate = new DayDate(onset.getTime());
		ExposeeRequest exposeeRequest = CryptoModule.getInstance(context).getSecretKeyForPublishing(onsetDate, exposeeAuthData);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.getBackendRepository(context)
				.addExposee(exposeeRequest,
						new CallbackListener<Void>() {
							@Override
							public void onSuccess(Void response) {
								appConfigManager.setAmIExposed(true);
								CryptoModule.getInstance(context).reset();
								callback.onSuccess(response);
							}

							@Override
							public void onError(Throwable throwable) {
								callback.onError(throwable);
							}
						});
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(false);
		appConfigManager.setReceivingEnabled(false);

		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_STOP);
		context.startService(intent);
		SyncWorker.stopSyncWorker(context);
	}

	public static IntentFilter getUpdateIntentFilter() {
		IntentFilter intentFilter = new IntentFilter(DP3T.UPDATE_INTENT_ACTION);
		return intentFilter;
	}

	public static void setCalibrationTestDeviceName(Context context, String name) {
		checkInit();
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(name);
	}

	public static String getCalibrationTestDeviceName(Context context) {
		checkInit();
		return AppConfigManager.getInstance(context).getCalibrationTestDeviceName();
	}

	public static void disableCalibrationTestDeviceName(Context context) {
		checkInit();
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(null);
	}

	public static void clearData(Context context, Runnable onDeleteListener) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled()) {
			throw new IllegalStateException("Tracking must be stopped for clearing the local data");
		}

		CryptoModule.getInstance(context).reset();
		appConfigManager.clearPreferences();
		Logger.clear();
		Database db = new Database(context);
		db.recreateTables(response -> onDeleteListener.run());
	}

	public static void exportDb(Context context, OutputStream targetOut, Runnable onExportedListener) {
		Database db = new Database(context);
		db.exportTo(context, targetOut, response -> onExportedListener.run());
	}

}
