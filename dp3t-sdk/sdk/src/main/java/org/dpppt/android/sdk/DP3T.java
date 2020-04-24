/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.SyncWorker;
import org.dpppt.android.sdk.internal.TracingService;
import org.dpppt.android.sdk.internal.backend.CallbackListener;
import org.dpppt.android.sdk.internal.backend.ResponseException;
import org.dpppt.android.sdk.internal.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.database.models.MatchedContact;
import org.dpppt.android.sdk.internal.gatt.BluetoothServiceStatus;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.dpppt.android.sdk.internal.util.ProcessUtil;

public class DP3T {

	private static final String TAG = "DP3T Interface";

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

	private static void checkInit() throws IllegalStateException {
		if (appId == null) {
			throw new IllegalStateException("You have to call DP3T.init() in your application onCreate()");
		}
	}

	public static void start(Context context) {
		start(context, true, true);
	}

	protected static void start(Context context, boolean advertise, boolean receive) {
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
		BroadcastHelper.sendUpdateBroadcast(context);
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
		Collection<TracingStatus.ErrorState> errorStates = checkTracingStatus(context);
		List<MatchedContact> matchedContacts = database.getMatchedContacts();
		InfectionStatus infectionStatus;
		if (appConfigManager.getIAmInfected()) {
			infectionStatus = InfectionStatus.INFECTED;
		} else if (matchedContacts.size() > 0) {
			infectionStatus = InfectionStatus.EXPOSED;
		} else {
			infectionStatus = InfectionStatus.HEALTHY;
		}
		return new TracingStatus(
				database.getContacts().size(),
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				appConfigManager.getLastSyncDate(),
				infectionStatus,
				matchedContacts,
				errorStates
		);
	}

	private static Collection<TracingStatus.ErrorState> checkTracingStatus(Context context) {
		Set<TracingStatus.ErrorState> errors = new HashSet<>();

		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			errors.add(TracingStatus.ErrorState.BLE_NOT_SUPPORTED);
		} else {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				errors.add(TracingStatus.ErrorState.BLE_DISABLED);
			}
		}

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		boolean batteryOptimizationsDeactivated =
				powerManager == null || powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
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

		if (!errors.contains(TracingStatus.ErrorState.BLE_DISABLED)) {
			BluetoothServiceStatus bluetoothServiceStatus = BluetoothServiceStatus.getInstance(context);
			switch (bluetoothServiceStatus.getAdvertiseStatus()) {
				case BluetoothServiceStatus.ADVERTISE_OK:
					// ok
					break;
				case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
				case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
					errors.add(TracingStatus.ErrorState.BLE_INTERNAL_ERROR);
					break;
				case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
					errors.add(TracingStatus.ErrorState.BLE_NOT_SUPPORTED);
					break;
				case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
				case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
				default:
					errors.add(TracingStatus.ErrorState.BLE_ADVERTISING_ERROR);
					break;
			}
			switch (bluetoothServiceStatus.getScanStatus()) {
				case BluetoothServiceStatus.SCAN_OK:
					// ok
					break;
				case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
					errors.add(TracingStatus.ErrorState.BLE_INTERNAL_ERROR);
					break;
				case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
					errors.add(TracingStatus.ErrorState.BLE_NOT_SUPPORTED);
					break;
				case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
				case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
				default:
					errors.add(TracingStatus.ErrorState.BLE_SCANNER_ERROR);
					break;
			}
		}

		return errors;
	}

	public static void sendIAmInfected(Context context, Date onset, ExposeeAuthMethod exposeeAuthMethod,
			CallbackListener<Void> callback) {
		checkInit();

		DayDate onsetDate = new DayDate(onset.getTime());
		ExposeeRequest exposeeRequest = CryptoModule.getInstance(context).getSecretKeyForPublishing(onsetDate, exposeeAuthMethod);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		try {
			appConfigManager.getBackendReportRepository(context).addExposee(exposeeRequest, exposeeAuthMethod,
					new CallbackListener<Void>() {
						@Override
						public void onSuccess(Void response) {
							appConfigManager.setIAmInfected(true);
							CryptoModule.getInstance(context).reset();
							callback.onSuccess(response);
						}

						@Override
						public void onError(Throwable throwable) {
							callback.onError(throwable);
						}
					});
		} catch (IllegalStateException e) {
			callback.onError(e);
			Logger.e(TAG, e);
		}
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(false);
		appConfigManager.setReceivingEnabled(false);

		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_STOP);
		context.startService(intent);
		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(DP3T.UPDATE_INTENT_ACTION);
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

}
