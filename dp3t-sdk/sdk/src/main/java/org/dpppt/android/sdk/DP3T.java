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
import android.content.Context;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.tasks.OnSuccessListener;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethodJson;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.ErrorHelper;
import org.dpppt.android.sdk.internal.SyncWorker;
import org.dpppt.android.sdk.internal.backend.CertificatePinning;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.database.models.ExposureDay;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.util.DayDate;

import okhttp3.CertificatePinner;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class DP3T {

	private static final String TAG = "DP3T Interface";

	public static final String ACTION_UPDATE = "org.dpppt.android.sdk.ACTION_UPDATE";
	public static final String ACTION_UPDATE_ERRORS = "org.dpppt.android.sdk.ACTION_UPDATE_ERRORS";

	public static final int REQUEST_CODE_START_CONFIRMATION = 391;
	public static final int REQUEST_CODE_EXPORT_KEYS = 392;

	private static String appId;

	public static void init(Context context, String appId, PublicKey signaturePublicKey) {
		init(context, appId, false, signaturePublicKey);
	}

	public static void init(Context context, String appId, boolean enableDevDiscoveryMode,
			PublicKey signaturePublicKey) {
		DP3T.appId = appId;
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAppId(appId);
		appConfigManager.setDevDiscoveryModeEnabled(enableDevDiscoveryMode);
		appConfigManager.triggerLoad();

		executeInit(context, signaturePublicKey);
	}

	public static void init(Context context, ApplicationInfo applicationInfo, PublicKey signaturePublicKey) {
		DP3T.appId = applicationInfo.getAppId();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setManualApplicationInfo(applicationInfo);

		executeInit(context, signaturePublicKey);
	}

	private static void executeInit(Context context, PublicKey signaturePublicKey) {
		SyncWorker.setBucketSignaturePublicKey(signaturePublicKey);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
		googleExposureClient.setParams(null);

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

	public static void start(Activity activity) {
		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(activity);
		googleExposureClient.setParams(null);
		googleExposureClient.startWithConfirmation(activity, REQUEST_CODE_START_CONFIRMATION,
				() -> {
					start(activity, true, true);
				},
				e -> {
					// TODO: publish error status?
				});
	}

	protected static void start(Context context, boolean advertise, boolean receive) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(advertise);
		appConfigManager.setReceivingEnabled(receive);
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
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SQLiteException | SignatureException ignored) {
			// has been handled upstream
		}
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		Collection<TracingStatus.ErrorState> errorStates = ErrorHelper.checkTracingErrorStatus(context);
		InfectionStatus infectionStatus;
		int contactCount = 0; // TODO: contactCount
		List<ExposureDay> exposureDays = new ArrayList<>(); // TODO: exposureDays
		if (appConfigManager.getIAmInfected()) {
			infectionStatus = InfectionStatus.INFECTED;
		} else if (exposureDays.size() > 0) {
			infectionStatus = InfectionStatus.EXPOSED;
		} else {
			infectionStatus = InfectionStatus.HEALTHY;
		}
		return new TracingStatus(
				contactCount,
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				appConfigManager.getLastSyncDate(),
				infectionStatus,
				exposureDays,
				errorStates
		);
	}

	public static void sendIAmInfected(Context context, Date onset, ExposeeAuthMethod exposeeAuthMethod,
			ResponseCallback<Void> callback) {
		checkInit();

		DayDate onsetDate = new DayDate(onset.getTime());

		GoogleExposureClient.getInstance(context)
				.getTemporaryExposureKeyHistory((Activity) context, REQUEST_CODE_EXPORT_KEYS,
						new OnSuccessListener<List<TemporaryExposureKey>>() {
							@Override
							public void onSuccess(List<TemporaryExposureKey> temporaryExposureKeys) {
								Logger.i("Keys", temporaryExposureKeys.toString());

								GaenRequest exposeeListRequest = new GaenRequest();
								ArrayList<GaenKey> keys = new ArrayList<>();
								for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
									keys.add(new GaenKey(toBase64(temporaryExposureKey.getKeyData()),
											temporaryExposureKey.getRollingStartIntervalNumber(),
											temporaryExposureKey.getRollingPeriod(),
											temporaryExposureKey.getTransmissionRiskLevel()));
								}
								exposeeListRequest.setGaenKeys(keys);

								AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
								try {
									appConfigManager.getBackendReportRepository(context)
											.addGaenExposee(exposeeListRequest, exposeeAuthMethod,
													new ResponseCallback<Void>() {
														@Override
														public void onSuccess(Void response) {
															appConfigManager.setIAmInfected(true);
															//TODO can we reset?
															stop(context);
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
						}, new Consumer<Exception>() {
							@Override
							public void accept(Exception e) {
								callback.onError(e);
								Logger.e(TAG, e);
							}
						});
	}

	public static void sendFakeInfectedRequest(Context context, Date onset, ExposeeAuthMethod exposeeAuthMethod)
			throws NoSuchAlgorithmException, IOException {
		checkInit();

		DayDate onsetDate = new DayDate(onset.getTime());
		ExposeeAuthMethodJson jsonAuthMethod = null;
		if (exposeeAuthMethod instanceof ExposeeAuthMethodJson) {
			jsonAuthMethod = (ExposeeAuthMethodJson) exposeeAuthMethod;
		}

		// TODO: fake request
		/*ExposeeRequest exposeeRequest = new ExposeeRequest(toBase64(CryptoModule.getInstance(context).getNewRandomKey()),
				onsetDate.getStartOfDayTimestamp(), 1, jsonAuthMethod);
		AppConfigManager.getInstance(context).getBackendReportRepository(context)
				.addExposeeSync(exposeeRequest, exposeeAuthMethod);*/
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(false);
		appConfigManager.setReceivingEnabled(false);

		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static void setMatchingParameters(Context context, float contactAttenuationThreshold, int numberOfWindowsForExposure) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setContactAttenuationThreshold(contactAttenuationThreshold);
		appConfigManager.setNumberOfWindowsForExposure(numberOfWindowsForExposure);
	}

	public static void setCertificatePinner(@NonNull CertificatePinner certificatePinner) {
		CertificatePinning.setCertificatePinner(certificatePinner);
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(DP3T.ACTION_UPDATE);
	}

	public static void clearData(Context context, Runnable onDeleteListener) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled()) {
			throw new IllegalStateException("Tracking must be stopped for clearing the local data");
		}

		appConfigManager.clearPreferences();
		Logger.clear();
	}

}
