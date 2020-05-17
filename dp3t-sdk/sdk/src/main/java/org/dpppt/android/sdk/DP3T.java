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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.*;
import org.dpppt.android.sdk.internal.backend.CertificatePinning;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.models.ExposureDay;
import org.dpppt.android.sdk.util.DateUtil;

import okhttp3.CertificatePinner;

public class DP3T {

	private static final String TAG = "DP3T Interface";

	public static final String ACTION_UPDATE = "org.dpppt.android.sdk.ACTION_UPDATE";
	public static final String ACTION_UPDATE_ERRORS = "org.dpppt.android.sdk.ACTION_UPDATE_ERRORS";

	public static final int REQUEST_CODE_START_CONFIRMATION = 391;
	public static final int REQUEST_CODE_EXPORT_KEYS = 392;

	private static String appId;
	private static String userAgent = "dp3t-sdk-android";

	private static PendingStartCallbacks pendingStartCallbacks;
	private static PendingIAmInfectedRequest pendingIAmInfectedRequest;

	public static void init(Context context, ApplicationInfo applicationInfo, PublicKey signaturePublicKey) {
		DP3T.appId = applicationInfo.getAppId();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setManualApplicationInfo(applicationInfo);

		executeInit(context.getApplicationContext(), signaturePublicKey);
	}

	private static void executeInit(Context context, PublicKey signaturePublicKey) {
		SyncWorker.setBucketSignaturePublicKey(signaturePublicKey);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
		googleExposureClient
				.setParams(appConfigManager.getAttenuationThresholdLow(), appConfigManager.getAttenuationThresholdMedium());

		BroadcastReceiver bluetoothStateChangeReceiver = new BluetoothStateBroadcastReceiver();
		IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(bluetoothStateChangeReceiver, bluetoothFilter);

		BroadcastReceiver locationServiceStateChangeReceiver = new LocationServiceBroadcastReceiver();
		IntentFilter locationServiceFilter = new IntentFilter(LocationManager.MODE_CHANGED_ACTION);
		context.registerReceiver(locationServiceStateChangeReceiver, locationServiceFilter);

		BroadcastReceiver tracingErrorsChangeReceiver = new TracingErrorsBroadcastReceiver();
		IntentFilter tracingErrorsFilter = new IntentFilter(DP3T.ACTION_UPDATE_ERRORS);
		context.registerReceiver(tracingErrorsChangeReceiver, tracingErrorsFilter);
	}

	private static void checkInit() throws IllegalStateException {
		if (appId == null) {
			throw new IllegalStateException("You have to call DP3T.init() in your Application.onCreate()");
		}
	}

	public static void start(Activity activity, Runnable successCallback, Consumer<Exception> errorCallback,
			Runnable cancelledCallback) {
		pendingStartCallbacks = new PendingStartCallbacks(successCallback, errorCallback, cancelledCallback);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(activity);
		googleExposureClient.start(activity, REQUEST_CODE_START_CONFIRMATION,
				() -> {
					resetStartCallbacks();
					startInternal(activity);
					successCallback.run();
				},
				e -> {
					resetStartCallbacks();
					errorCallback.accept(e);
				});
	}

	private static void resetStartCallbacks() {
		pendingStartCallbacks = null;
	}

	public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQUEST_CODE_START_CONFIRMATION) {
			if (resultCode == Activity.RESULT_OK) {
				start(activity, pendingStartCallbacks.successCallback, pendingStartCallbacks.errorCallback,
						pendingStartCallbacks.cancelledCallback);
			} else {
				Runnable cancelledCallback = pendingStartCallbacks.cancelledCallback;
				resetStartCallbacks();
				cancelledCallback.run();
			}
			return true;
		} else if (requestCode == REQUEST_CODE_EXPORT_KEYS) {
			if (resultCode == Activity.RESULT_OK) {
				executeIAmInfected(activity);
			} else {
				reportFailedIAmInfected(new CancellationException("user denied key export"));
			}
			return true;
		}
		return false;
	}

	private static void startInternal(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setTracingEnabled(true);
		SyncWorker.startSyncWorker(context);
		BroadcastHelper.sendUpdateAndErrorBroadcast(context);
	}

	public static boolean isTracingEnabled(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return appConfigManager.isTracingEnabled();
	}

	public static void sync(Context context) {
		checkInit();
		try {
			SyncWorker.doSync(context);
		} catch (Exception ignored) {
			// has been handled upstream
		}
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		Collection<TracingStatus.ErrorState> errorStates = ErrorHelper.checkTracingErrorStatus(context);
		InfectionStatus infectionStatus;
		List<ExposureDay> exposureDays = ExposureDayStorage.getInstance(context).getExposureDays();
		if (appConfigManager.getIAmInfected()) {
			infectionStatus = InfectionStatus.INFECTED;
		} else if (exposureDays.size() > 0) {
			infectionStatus = InfectionStatus.EXPOSED;
		} else {
			infectionStatus = InfectionStatus.HEALTHY;
		}
		return new TracingStatus(
				appConfigManager.isTracingEnabled(),
				appConfigManager.getLastSyncDate(),
				infectionStatus,
				exposureDays,
				errorStates
		);
	}

	public static void sendIAmInfected(Activity activity, Date onset, ExposeeAuthMethod exposeeAuthMethod,
			ResponseCallback<Void> callback) {
		checkInit();

		pendingIAmInfectedRequest = new PendingIAmInfectedRequest(onset, exposeeAuthMethod, callback);

		executeIAmInfected(activity);
	}

	private static void executeIAmInfected(Activity activity) {
		if (pendingIAmInfectedRequest == null) {
			throw new IllegalStateException("pendingIAmInfectedRequest must be set before calling executeIAmInfected()");
		}
		DayDate onsetDate = new DayDate(pendingIAmInfectedRequest.onset.getTime());

		GoogleExposureClient.getInstance(activity)
				.getTemporaryExposureKeyHistory(activity, REQUEST_CODE_EXPORT_KEYS,
						temporaryExposureKeys -> {
							List<TemporaryExposureKey> filteredKeys = new ArrayList<>();
							for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
								if (temporaryExposureKey.getRollingStartIntervalNumber() >=
										DateUtil.getRollingStartNumberForDate(onsetDate)) {
									filteredKeys.add(temporaryExposureKey);
								}
							}
							int delayedKeyDate = DateUtil.getCurrentRollingStartNumber();
							GaenRequest exposeeListRequest = new GaenRequest(filteredKeys, delayedKeyDate);

							AppConfigManager appConfigManager = AppConfigManager.getInstance(activity);
							try {
								appConfigManager.getBackendReportRepository(activity)
										.addGaenExposee(exposeeListRequest, pendingIAmInfectedRequest.exposeeAuthMethod,
												new ResponseCallback<String>() {
													@Override
													public void onSuccess(String authToken) {
														PendingKeyUploadStorage.PendingKey delayedKey =
																new PendingKeyUploadStorage.PendingKey(
																		delayedKeyDate,
																		authToken,
																		0);
														PendingKeyUploadStorage.getInstance(activity).addPendingKey(delayedKey);
														appConfigManager.setIAmInfected(true);
														pendingIAmInfectedRequest.callback.onSuccess(null);
														pendingIAmInfectedRequest = null;
													}

													@Override
													public void onError(Throwable throwable) {
														reportFailedIAmInfected(throwable);
													}
												});
							} catch (IllegalStateException e) {
								reportFailedIAmInfected(e);
							}
						}, e -> {
							reportFailedIAmInfected(e);
						});
	}

	private static void reportFailedIAmInfected(Throwable e) {
		if (pendingIAmInfectedRequest == null) {
			throw new IllegalStateException("pendingIAmInfectedRequest must be set before calling reportFailedIAmInfected()");
		}
		pendingIAmInfectedRequest.callback.onError(e);
		Logger.e(TAG, e);
		pendingIAmInfectedRequest = null;
	}

	public static void sendFakeInfectedRequest(Context context, ExposeeAuthMethod exposeeAuthMethod) {
		checkInit();

		int delayedKeyDate = DateUtil.getCurrentRollingStartNumber();
		GaenRequest exposeeListRequest = new GaenRequest(new ArrayList<>(), delayedKeyDate);
		exposeeListRequest.setFake(1);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		try {
			appConfigManager.getBackendReportRepository(context)
					.addGaenExposee(exposeeListRequest, exposeeAuthMethod,
							new ResponseCallback<String>() {
								@Override
								public void onSuccess(String authToken) {
									PendingKeyUploadStorage.PendingKey delayedKey = new PendingKeyUploadStorage.PendingKey(
											delayedKeyDate,
											authToken,
											1);
									PendingKeyUploadStorage.getInstance(context).addPendingKey(delayedKey);
									Logger.d(TAG, "successfully sent fake request");
								}

								@Override
								public void onError(Throwable throwable) {
									Logger.d(TAG, "failed to send fake request");
								}
							});
		} catch (IllegalStateException e) {
			Logger.d(TAG, "failed to send fake request: " + e.getLocalizedMessage());
		}
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setTracingEnabled(false);

		GoogleExposureClient.getInstance(context).stop();

		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateAndErrorBroadcast(context);
	}

	public static void resetExposureDays(Context context) {
		ExposureDayStorage.getInstance(context).clear();
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static boolean getIAmInfectedIsResettable(Context context) {
		return AppConfigManager.getInstance(context).getIAmInfectedIsResettable();
	}

	public static void resetInfectionStatus(Context context) {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.getIAmInfectedIsResettable()) {
			appConfigManager.setIAmInfected(false);
			appConfigManager.setIAmInfectedIsResettable(false);
			BroadcastHelper.sendUpdateBroadcast(context);
		} else {
			throw new IllegalStateException("InfectionStatus can only be reseted if getIAmInfectedIsResettable() returns true");
		}
	}

	public static void setCertificatePinner(@NonNull CertificatePinner certificatePinner) {
		CertificatePinning.setCertificatePinner(certificatePinner);
	}

	public static void setUserAgent(String userAgent) {
		DP3T.userAgent = userAgent;
	}

	public static String getUserAgent() {
		return userAgent;
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(DP3T.ACTION_UPDATE);
	}

	public static void clearData(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.isTracingEnabled()) {
			throw new IllegalStateException("Tracing must be stopped to clear the local data");
		}

		appConfigManager.clearPreferences();
		ExposureDayStorage.getInstance(context).clear();
		PendingKeyUploadStorage.getInstance(context).clear();
		Logger.clear();
	}


	private static class PendingStartCallbacks {
		private final Runnable successCallback;
		private final Consumer<Exception> errorCallback;
		private final Runnable cancelledCallback;

		private PendingStartCallbacks(Runnable successCallback, Consumer<Exception> errorCallback,
				Runnable cancelledCallback) {
			this.successCallback = successCallback;
			this.errorCallback = errorCallback;
			this.cancelledCallback = cancelledCallback;
		}

	}


	private static class PendingIAmInfectedRequest {
		private Date onset;
		private ExposeeAuthMethod exposeeAuthMethod;
		private ResponseCallback<Void> callback;

		private PendingIAmInfectedRequest(Date onset, ExposeeAuthMethod exposeeAuthMethod, ResponseCallback<Void> callback) {
			this.onset = onset;
			this.exposeeAuthMethod = exposeeAuthMethod;
			this.callback = callback;
		}

	}

}
