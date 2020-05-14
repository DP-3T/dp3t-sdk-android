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
import androidx.core.util.Consumer;

import java.io.File;
import java.util.List;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.exposurenotification.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.dpppt.android.sdk.internal.logger.Logger;

public class GoogleExposureClient {

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

	public void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.start()
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
								Logger.e(TAG, "Error calling startResolutionForResult()");
							}
						}
					}
					Logger.e(TAG, e);
					errorCallback.accept(e);
				});
	}

	public void stop() {
		exposureNotificationClient.stop()
				.addOnSuccessListener(nothing -> Logger.i(TAG, "stopped successfully"))
				.addOnFailureListener(e -> Logger.e(TAG, e));
	}

	public Task<Boolean> isEnabled() {
		return exposureNotificationClient.isEnabled();
	}

	public void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
			OnSuccessListener<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(successCallback)
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						ApiException apiException = (ApiException) e;
						if (apiException.getStatusCode() == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
							try {
								apiException.getStatus().startResolutionForResult(activity, resolutionRequestCode);
								return;
							} catch (IntentSender.SendIntentException e2) {
								Logger.e(TAG, "Error calling startResolutionForResult()");
							}
						}
					}
					Logger.e(TAG, e);
					errorCallback.accept(e);
				});
	}

	public void setParams() {
		// TODO
		// default values
		exposureConfiguration = new ExposureConfiguration.ExposureConfigurationBuilder()
				.setMinimumRiskScore(1)
				.setAttenuationScores(new int[] { 1, 1, 1, 1, 1, 1, 1, 1 })
				.setAttenuationWeight(100)
				.setDaysSinceLastExposureWeight(0)
				.setDurationWeight(0)
				.setTransmissionRiskWeight(0)
				.setDurationAtAttenuationThresholds(new int[] { 20, 40 })
				.build();
	}

	public ExposureConfiguration getExposureConfiguration() {
		return exposureConfiguration;
	}

	public void provideDiagnosisKeys(List<File> keys, String token, OnFailureListener onFailureListener) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}

		exposureNotificationClient.provideDiagnosisKeys(keys, exposureConfiguration, token)
				.addOnSuccessListener(nothing -> {
					Logger.d(TAG, "inserted keys successfully");
					// ok
				})
				.addOnFailureListener(onFailureListener);
	}

	public Task<ExposureSummary> getExposureSummary(String token) {
		return exposureNotificationClient.getExposureSummary(token);
	}

	public Task<List<ExposureInformation>> getExposureInformation(String token) {
		return exposureNotificationClient.getExposureInformation(token);
	}

}
