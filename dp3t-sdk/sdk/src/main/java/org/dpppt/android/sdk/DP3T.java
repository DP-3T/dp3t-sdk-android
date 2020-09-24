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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CancellationException;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.*;
import org.dpppt.android.sdk.internal.backend.CertificatePinning;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.history.HistoryDatabase;
import org.dpppt.android.sdk.internal.history.HistoryEntry;
import org.dpppt.android.sdk.internal.history.HistoryEntryType;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateCache;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.storage.ErrorNotificationStorage;
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage;
import org.dpppt.android.sdk.internal.storage.PendingKeyUploadStorage;
import org.dpppt.android.sdk.internal.storage.models.PendingKey;
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

	private static final int HISTORY_KEEP_FOR_DAYS = 14;

	private static boolean initialized = false;

	private static String appId;
	private static String userAgent = "dp3t-sdk-android";

	private static PendingStartCallbacks pendingStartCallbacks;
	private static PendingIAmInfectedRequest pendingIAmInfectedRequest;

	public static void init(Context context, ApplicationInfo applicationInfo, PublicKey signaturePublicKey) {
		init(context, applicationInfo, signaturePublicKey, false);
	}

	public static void init(Context context, ApplicationInfo applicationInfo, PublicKey signaturePublicKey, boolean devHistory) {
		DP3T.appId = applicationInfo.getAppId();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setManualApplicationInfo(applicationInfo);
		appConfigManager.setDevHistory(devHistory);
		SyncWorker.setBucketSignaturePublicKey(signaturePublicKey);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
		googleExposureClient
				.setParams(appConfigManager.getAttenuationThresholdLow(), appConfigManager.getAttenuationThresholdMedium());

		executeInit(context.getApplicationContext(), appConfigManager);

		initialized = true;
	}

	private static void executeInit(Context context, AppConfigManager appConfigManager) {
		if (initialized) {
			return;
		}

		context.registerReceiver(
				new BluetoothStateBroadcastReceiver(),
				new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
		);
		if (!ErrorHelper.deviceSupportsLocationlessScanning(context)) {
			context.registerReceiver(
					new LocationServiceBroadcastReceiver(),
					new IntentFilter(LocationManager.MODE_CHANGED_ACTION)
			);
		}
		context.registerReceiver(
				new BatteryOptimizationBroadcastReceiver(),
				new IntentFilter(BatteryOptimizationBroadcastReceiver.ACTION_POWER_SAVE_WHITELIST_CHANGED)
		);
		context.registerReceiver(
				new TracingErrorsBroadcastReceiver(),
				new IntentFilter(DP3T.ACTION_UPDATE_ERRORS)
		);

		GaenStateHelper.invalidateGaenAvailability(context);
		GaenStateHelper.invalidateGaenEnabled(context);

		if (appConfigManager.isTracingEnabled()) {
			SyncWorker.startSyncWorker(context);
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

	public static boolean isInitialized() {
		return initialized;
	}

	private static void checkInit() throws IllegalStateException {
		if (!initialized) {
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
					GaenStateCache.setGaenEnabled(true, null, activity);
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
			if (pendingStartCallbacks == null) {
				Logger.w(TAG, "onActivityResult: start confirmation, missing callback");
				return false;
			}
			if (resultCode == Activity.RESULT_OK) {
				start(activity, pendingStartCallbacks.successCallback, pendingStartCallbacks.errorCallback,
						pendingStartCallbacks.cancelledCallback);
			} else {
				Logger.w(TAG, "onActivityResult: start confirmation, resultCode=" + resultCode);
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
			new SyncWorker.SyncImpl(context).doSync();
		} catch (Exception ignored) {
			// has been handled upstream
		}
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		GaenStateHelper.invalidateGaenEnabled(context);
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		Collection<TracingStatus.ErrorState> errorStates = ErrorHelper.checkTracingErrorStatus(context, appConfigManager);
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

	public static void checkGaenAvailability(Context context, Consumer<GaenAvailability> availabilityCallback) {
		GaenStateHelper.checkGaenAvailability(context, availabilityCallback);
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
							int delayedKeyDate = DateUtil.getCurrentRollingStartNumber();
							boolean delayedKeyAlreadyPresent = false;
							for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
								if (temporaryExposureKey.getRollingStartIntervalNumber() >=
										DateUtil.getRollingStartNumberForDate(onsetDate)) {
									filteredKeys.add(temporaryExposureKey);
									if (temporaryExposureKey.getRollingStartIntervalNumber() == delayedKeyDate) {
										delayedKeyAlreadyPresent = true;
									}
								}
							}
							GaenRequest exposeeListRequest = new GaenRequest(filteredKeys, delayedKeyDate);

							AppConfigManager appConfigManager = AppConfigManager.getInstance(activity);
							try {
								boolean finalDelayedKeyAlreadyPresent = delayedKeyAlreadyPresent;
								appConfigManager.getBackendReportRepository(activity)
										.addGaenExposee(exposeeListRequest, pendingIAmInfectedRequest.exposeeAuthMethod,
												new ResponseCallback<String>() {
													@Override
													public void onSuccess(String authToken) {
														//if the currentDay key was already released (because of same day TEK
														// release) we do a fake request the next day, otherwise we upload todays
														// key tomorrow
														PendingKey delayedKey = new PendingKey(delayedKeyDate, authToken,
																finalDelayedKeyAlreadyPresent ? 1 : 0);
														PendingKeyUploadStorage.getInstance(activity).addPendingKey(delayedKey);
														appConfigManager.setIAmInfected(true);
														if (finalDelayedKeyAlreadyPresent) {
															DP3T.stop(activity);
															appConfigManager.setIAmInfectedIsResettable(true);
														}
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
		Logger.e(TAG, "reportFailedIAmInfected", e);
		pendingIAmInfectedRequest.callback.onError(e);
		pendingIAmInfectedRequest = null;
	}

	public static void sendFakeInfectedRequest(Context context, ExposeeAuthMethod exposeeAuthMethod, Runnable successCallback,
			Runnable errorCallback) {
		checkInit();

		int delayedKeyDate = DateUtil.getCurrentRollingStartNumber();
		GaenRequest exposeeListRequest = new GaenRequest(new ArrayList<>(), delayedKeyDate);
		exposeeListRequest.setFake(1);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		boolean devHistory = appConfigManager.getDevHistory();
		try {
			appConfigManager.getBackendReportRepository(context)
					.addGaenExposee(exposeeListRequest, exposeeAuthMethod,
							new ResponseCallback<String>() {
								@Override
								public void onSuccess(String authToken) {
									PendingKey delayedKey = new PendingKey(delayedKeyDate, authToken, 1);
									PendingKeyUploadStorage.getInstance(context).addPendingKey(delayedKey);
									Logger.d(TAG, "successfully sent fake request");
									if (devHistory) {
										HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
										historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.FAKE_REQUEST, null, true,
												System.currentTimeMillis()));
									}
									if (successCallback != null) successCallback.run();
								}

								@Override
								public void onError(Throwable throwable) {
									Logger.d(TAG, "failed to send fake request");
									if (devHistory) {
										HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
										String status = throwable instanceof StatusCodeException ?
														String.valueOf(((StatusCodeException) throwable).getCode()) : "NETW";
										historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.FAKE_REQUEST, status, false,
												System.currentTimeMillis()));
									}
									if (errorCallback != null) errorCallback.run();
								}
							});
		} catch (IllegalStateException e) {
			Logger.d(TAG, "failed to send fake request: " + e.getLocalizedMessage());
			if (devHistory) {
				HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
				historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.FAKE_REQUEST, "SYST", false,
						System.currentTimeMillis()));
			}
			if (errorCallback != null) errorCallback.run();
		}
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setTracingEnabled(false);

		GoogleExposureClient.getInstance(context).stop();

		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateAndErrorBroadcast(context);

		GaenStateHelper.invalidateGaenEnabled(context);
	}

	public static void resetExposureDays(Context context) {
		ExposureDayStorage.getInstance(context).resetExposureDays();
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
			throw new IllegalStateException("InfectionStatus can only be reset if getIAmInfectedIsResettable() returns true");
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

	public static void setSyncErrorGracePeriod(long gracePeriodMillis) {
		SyncErrorState.getInstance().setSyncErrorGracePeriod(gracePeriodMillis);
	}

	public static void setErrorNotificationGracePeriod(long gracePeriodMillis) {
		SyncErrorState.getInstance().setErrorNotificationGracePeriod(gracePeriodMillis);
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(DP3T.ACTION_UPDATE);
	}

	public static void setMatchingParameters(Context context, int attenuationThresholdLow, int attenuationThresholdMedium,
			float attenuationFactorLow, float attenuationFactorMedium, int minDurationForExposure) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAttenuationThresholds(attenuationThresholdLow, attenuationThresholdMedium);
		appConfigManager.setAttenuationFactorLow(attenuationFactorLow);
		appConfigManager.setAttenuationFactorMedium(attenuationFactorMedium);
		appConfigManager.setMinDurationForExposure(minDurationForExposure);
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
		ErrorNotificationStorage.getInstance(context).clear();
		Logger.clear();
	}

	public static void addClientOpenedToHistory(Context context) {
		HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
		Calendar calendar = new GregorianCalendar();
		historyDatabase.addEntry(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, calendar.getTimeInMillis()));
		calendar.add(Calendar.DAY_OF_YEAR, -1 * HISTORY_KEEP_FOR_DAYS);
		historyDatabase.clearBefore(calendar.getTimeInMillis());
	}

	public static void addWorkerStartedToHistory(Context context, String workerName) {
		if (AppConfigManager.getInstance(context).getDevHistory()) {
			HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
			historyDatabase
					.addEntry(new HistoryEntry(HistoryEntryType.WORKER_STARTED, workerName, true, System.currentTimeMillis()));
		}
	}

	public static List<HistoryEntry> getHistoryEntries(Context context) {
		return HistoryDatabase.getInstance(context).getEntries();
	}


	private static class PendingStartCallbacks {
		private final Runnable successCallback;
		private final Consumer<Exception> errorCallback;
		private final Runnable cancelledCallback;

		private PendingStartCallbacks(Runnable successCallback, Consumer<Exception> errorCallback, Runnable cancelledCallback) {
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
