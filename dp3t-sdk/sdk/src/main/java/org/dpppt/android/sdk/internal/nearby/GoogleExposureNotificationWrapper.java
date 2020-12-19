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
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.platformapi.PlatformAPIWrapper;

public class GoogleExposureNotificationWrapper extends PlatformAPIWrapper {

	private static final String TAG = "GoogleExposureClient";

	private final ExposureNotificationClient exposureNotificationClient;

	public GoogleExposureNotificationWrapper(Context context) {
		exposureNotificationClient = Nearby.getExposureNotificationClient(context);
	}

	@Override
	public void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Runnable cancelledCallback,
			Consumer<Exception> errorCallback) {
		exposureNotificationClient.start()
				.addOnSuccessListener(nothing -> {
					Logger.i(TAG, "start: started successfully");
					successCallback.run();
				})
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						ApiException apiException = (ApiException) e;
						if (apiException.getStatusCode() == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
							try {
								Logger.i(TAG, "start: resolution required");
								apiException.getStatus().startResolutionForResult(activity, resolutionRequestCode);
								return;
							} catch (IntentSender.SendIntentException e2) {
								Logger.e(TAG, "start: error calling startResolutionForResult()");
							}
						}
					}
					Logger.e(TAG, "start", e);
					errorCallback.accept(e);
				});
	}

	@Override
	public void stop() {
		exposureNotificationClient.stop()
				.addOnSuccessListener(nothing -> Logger.i(TAG, "stop: stopped successfully"))
				.addOnFailureListener(e -> Logger.e(TAG, "stop", e));
	}

	@Override
	public void isEnabled(Consumer<Boolean> successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.isEnabled()
				.addOnSuccessListener(enabled -> successCallback.accept(enabled))
				.addOnFailureListener(e -> errorCallback.accept(e));
	}

	@Override
	public void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
			Consumer<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(temporaryExposureKeys -> {
					Logger.d(TAG, "getTemporaryExposureKeyHistory: success");
					successCallback.accept(temporaryExposureKeys);
				})
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						ApiException apiException = (ApiException) e;
						if (apiException.getStatusCode() == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
							try {
								Logger.i(TAG, "getTemporaryExposureKeyHistory: resolution required");
								apiException.getStatus().startResolutionForResult(activity, resolutionRequestCode);
								return;
							} catch (IntentSender.SendIntentException e2) {
								Logger.e(TAG, "getTemporaryExposureKeyHistory: error calling startResolutionForResult()");
							}
						}
					}
					Logger.e(TAG, "getTemporaryExposureKeyHistory", e);
					errorCallback.accept(e);
				});
	}

	@Override
	public List<TemporaryExposureKey> getTemporaryExposureKeyHistorySynchronous() throws Exception {
		final Object syncObject = new Object();
		Object[] results = new Object[] { null };

		synchronized (syncObject) {
			exposureNotificationClient.getTemporaryExposureKeyHistory()
					.addOnSuccessListener(list -> {
						results[0] = list;
						synchronized (syncObject) {
							syncObject.notifyAll();
						}
					})
					.addOnFailureListener(e -> {
						results[0] = e;
						synchronized (syncObject) {
							syncObject.notifyAll();
						}
					});

			syncObject.wait();
		}
		if (results[0] instanceof Exception) {
			throw (Exception) results[0];
		} else if (results[0] instanceof List) {
			return (List<TemporaryExposureKey>) results[0];
		} else {
			throw new IllegalStateException("either exception or result must be set");
		}
	}

	@Override
	public void provideDiagnosisKeys(List<File> keys, String token) throws Exception {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}

		final Object syncObject = new Object();
		Exception[] exceptions = new Exception[] { null };
		synchronized (syncObject) {
			exposureNotificationClient.provideDiagnosisKeys(keys, exposureConfiguration, token)
					.addOnSuccessListener(nothing -> {
						Logger.d(TAG, "provideDiagnosisKeys: inserted keys successfully for token " + token);
						synchronized (syncObject) {
							syncObject.notifyAll();
						}
					})
					.addOnFailureListener(e -> {
						Logger.e(TAG, "provideDiagnosisKeys for token " + token, e);
						exceptions[0] = e;
						synchronized (syncObject) {
							syncObject.notifyAll();
						}
					});

			syncObject.wait();
		}
		if (exceptions[0] != null) {
			throw exceptions[0];
		}
	}

	@Override
	public void getExposureSummary(String token, Consumer<ExposureSummary> successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.getExposureSummary(token)
				.addOnSuccessListener(exposureSummary -> successCallback.accept(exposureSummary))
				.addOnFailureListener(e -> errorCallback.accept(e));
	}

}
