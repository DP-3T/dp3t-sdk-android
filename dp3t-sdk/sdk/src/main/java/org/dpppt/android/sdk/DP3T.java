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
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.ErrorHelper;
import org.dpppt.android.sdk.internal.SyncWorker;
import org.dpppt.android.sdk.internal.backend.CertificatePinning;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.models.ExposeeAuthMethodJson;
import org.dpppt.android.sdk.models.ExposureDay;
import org.dpppt.android.sdk.util.DateUtil;

import okhttp3.CertificatePinner;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class DP3T {

	private static final String TAG = "DP3T Interface";

	public static final String ACTION_UPDATE = "org.dpppt.android.sdk.ACTION_UPDATE";
	public static final String ACTION_UPDATE_ERRORS = "org.dpppt.android.sdk.ACTION_UPDATE_ERRORS";

	public static final int REQUEST_CODE_START_CONFIRMATION = 391;
	public static final int REQUEST_CODE_EXPORT_KEYS = 392;

	private static String appId;

	private static PendingIAmInfectedRequest pendingIAmInfectedRequest;

	public static void init(Context context, ApplicationInfo applicationInfo, PublicKey signaturePublicKey) {
		DP3T.appId = applicationInfo.getAppId();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setManualApplicationInfo(applicationInfo);

		executeInit(context, signaturePublicKey);
	}

	private static void executeInit(Context context, PublicKey signaturePublicKey) {
		SyncWorker.setBucketSignaturePublicKey(signaturePublicKey);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
		googleExposureClient.setParams();
	}

	private static void checkInit() throws IllegalStateException {
		if (appId == null) {
			throw new IllegalStateException("You have to call DP3T.init() in your application onCreate()");
		}
	}

	public static void start(Activity activity) {
		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(activity);
		googleExposureClient.startWithConfirmation(activity, REQUEST_CODE_START_CONFIRMATION,
				() -> {
					startInternal(activity);
				},
				e -> {
					// TODO: publish error status?
				});
	}

	public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == DP3T.REQUEST_CODE_START_CONFIRMATION) {
			if (resultCode == Activity.RESULT_OK) {
				DP3T.start(activity);
			}
			return true;
		} else if (requestCode == REQUEST_CODE_EXPORT_KEYS) {
			if (resultCode == Activity.RESULT_OK) {
				executeIAmInfected();
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
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static boolean isStarted(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return appConfigManager.isTracingEnabled();
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
		List<ExposureDay> exposureDays = new ArrayList<>(); // TODO: exposureDays
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

		pendingIAmInfectedRequest = new PendingIAmInfectedRequest(activity, onset, exposeeAuthMethod, callback);

		executeIAmInfected();
	}

	private static void executeIAmInfected() {
		if (pendingIAmInfectedRequest == null) {
			throw new IllegalStateException("pendingIAmInfectedRequest must be set beforee calling executeIAmInfected()");
		}
		DayDate onsetDate = new DayDate(pendingIAmInfectedRequest.onset.getTime());

		GoogleExposureClient.getInstance(pendingIAmInfectedRequest.activity)
				.getTemporaryExposureKeyHistory(pendingIAmInfectedRequest.activity, REQUEST_CODE_EXPORT_KEYS,
						temporaryExposureKeys -> {
							Logger.i("Keys", temporaryExposureKeys.toString());

							ArrayList<GaenKey> keys = new ArrayList<>();
							for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
								keys.add(new GaenKey(toBase64(temporaryExposureKey.getKeyData()),
										temporaryExposureKey.getRollingStartIntervalNumber(),
										temporaryExposureKey.getRollingPeriod(),
										temporaryExposureKey.getTransmissionRiskLevel()));
							}
							while (keys.size() < 14) {
								keys.add(new GaenKey(toBase64(new byte[16]),
										DateUtil.getCurrentRollingStartNumber(),
										0,
										0));
							}
							GaenRequest exposeeListRequest = new GaenRequest(keys, DateUtil.getCurrentRollingStartNumber());
							exposeeListRequest.setGaenKeys(keys);

							AppConfigManager appConfigManager = AppConfigManager.getInstance(pendingIAmInfectedRequest.activity);
							try {
								appConfigManager.getBackendReportRepository(pendingIAmInfectedRequest.activity)
										.addGaenExposee(exposeeListRequest, pendingIAmInfectedRequest.exposeeAuthMethod,
												new ResponseCallback<Void>() {
													@Override
													public void onSuccess(Void response) {
														appConfigManager.setIAmInfected(true);
														//TODO can we reset?
														stop(pendingIAmInfectedRequest.activity);
														pendingIAmInfectedRequest.callback.onSuccess(response);
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
		appConfigManager.setTracingEnabled(false);

		GoogleExposureClient.getInstance(context).stop();

		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static void resetExposureDays() {
		// TODO
	}

	public static void resetInfectionStatus() {
		// TODO
	}

	public static void setCertificatePinner(@NonNull CertificatePinner certificatePinner) {
		CertificatePinning.setCertificatePinner(certificatePinner);
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
		Logger.clear();
	}


	private static class PendingIAmInfectedRequest {
		Activity activity;
		Date onset;
		ExposeeAuthMethod exposeeAuthMethod;
		ResponseCallback<Void> callback;

		public PendingIAmInfectedRequest(Activity activity, Date onset, ExposeeAuthMethod exposeeAuthMethod,
				ResponseCallback<Void> callback) {
			this.activity = activity;
			this.onset = onset;
			this.exposeeAuthMethod = exposeeAuthMethod;
			this.callback = callback;
		}

	}

}
