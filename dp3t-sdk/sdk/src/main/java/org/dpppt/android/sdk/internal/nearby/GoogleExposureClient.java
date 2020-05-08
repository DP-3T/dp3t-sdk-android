/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.exposurenotification.*;
import com.google.android.gms.tasks.Task;

import org.dpppt.android.sdk.internal.TracingController;
import org.dpppt.android.sdk.internal.backend.proto.GaenExposed;
import org.dpppt.android.sdk.internal.logger.Logger;

public class GoogleExposureClient implements TracingController {

	private static final String TAG = "GoogleExposureClient";

	private static GoogleExposureClient instance;

	private final ExposureNotificationClient exposureNotificationClient;

	private ExposureConfiguration exposureConfiguration;

	public static GoogleExposureClient getInstance(Context context) {
		if (instance == null) {
			instance = new GoogleExposureClient(context.getApplicationContext());
		}
		return instance;
	}

	private GoogleExposureClient(Context context) {
		exposureNotificationClient = Nearby.getExposureNotificationClient(context);
	}

	@Override
	public void setParams(Bundle extras) {
		// TODO
		// default values
		exposureConfiguration = new ExposureConfiguration.ExposureConfigurationBuilder()
				.setMinimumRiskScore(4)
				.setAttenuationScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setAttenuationWeight(50)
				.setDaysSinceLastExposureScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setDaysSinceLastExposureWeight(50)
				.setDurationScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setDurationWeight(50)
				.setTransmissionRiskScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setTransmissionRiskWeight(50)
				.build();
	}

	public void startWithConfirmation(Activity activity, int resolutionRequestCode,
			Runnable successCallback, Consumer<Exception> errorCallback) {
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}
		exposureNotificationClient.start(exposureConfiguration)
				.addOnSuccessListener(nothing -> {
					Logger.i(TAG, "started successfully");
					successCallback.run();
				})
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						ApiException apiException = (ApiException) e;
						if (apiException.getStatusCode() == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
							try {
								apiException.getStatus().startResolutionForResult(activity, resolutionRequestCode);
								return;
							} catch (IntentSender.SendIntentException e2) {
								Logger.e(TAG, "Error calling startResolutionForResult, sending to settings");
							}
						}
					}
					Logger.e(TAG, e);
					errorCallback.accept(e);
				});
	}

	@Override
	public void start() {
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}
		exposureNotificationClient.start(exposureConfiguration)
				.addOnSuccessListener(nothing -> Logger.i(TAG, "started successfully"))
				.addOnFailureListener(e -> {
					Logger.e(TAG, e);
					// TODO: add service error state? need to check when this can happen
				});
	}

	@Override
	public void stop() {
		exposureNotificationClient.stop()
				.addOnSuccessListener(nothing -> Logger.i(TAG, "stopped successfully"))
				.addOnFailureListener(e -> Logger.e(TAG, e));
	}

	public Task<Boolean> isEnabled() {
		return exposureNotificationClient.isEnabled();
	}

	public void getTemporaryExposureKeyHistory() {
		// TODO: provide callback
		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(temporaryExposureKeys -> Logger.i(TAG, "keys: " + temporaryExposureKeys.toString()))
				.addOnFailureListener(e -> Logger.e(TAG, e));
	}

	public void provideDiagnosisKeys(List<GaenExposed.Key> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}

		List<TemporaryExposureKey> temporaryExposureKeys = new ArrayList<>();
		for (GaenExposed.Key key : keys) {
			TemporaryExposureKey temporaryExposureKey = new TemporaryExposureKey.TemporaryExposureKeyBuilder()
					.setKeyData(key.getKeyData().toByteArray())
					.setRollingStartIntervalNumber(key.getRollingStartNumber())
					.setTransmissionRiskLevel(key.getTransmissionRiskLevel())
					.build();
			temporaryExposureKeys.add(temporaryExposureKey);
		}

		// TODO: 1. must wait for completion
		// TODO: 2. key list must not be longer than #getMaxDiagnosisKeys() (split into multiple batches otherwise)
		exposureNotificationClient.provideDiagnosisKeys(temporaryExposureKeys)
				.addOnSuccessListener(nothing -> {
					// ok
				})
				.addOnFailureListener(e -> {
					Logger.e(TAG, e);
					// TODO: add service error state
				});
	}

	public Task<Integer> getMaxDiagnosisKeysCount() {
		return exposureNotificationClient.getMaxDiagnosisKeyCount();
	}

	public Task<ExposureSummary> getExposureSummary() {
		return exposureNotificationClient.getExposureSummary();
	}

	public Task<List<ExposureInformation>> getExposureInformation() {
		return exposureNotificationClient.getExposureInformation();
	}

	public Task<Void> resetAllData() {
		return exposureNotificationClient.resetAllData();
	}

	public Task<Void> resetTemporaryExposureKey() {
		return exposureNotificationClient.resetTemporaryExposureKey();
	}

	@Override
	public void restartClient() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void restartServer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy() {
		instance = null;
	}

}
