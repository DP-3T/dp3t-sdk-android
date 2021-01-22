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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.exposurenotification.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.dpppt.android.sdk.internal.logger.Logger;

public class GoogleExposureClient {

	private static final String TAG = "GoogleExposureClient";

	private static final String EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET = "either exception or result must be set";

	private static final long TIMEOUT_EXPOSURE_CLIENT_CALL_MS = 5 * 60 * 1000L;//5min

	private static GoogleExposureClient instance;

	private final ExposureNotificationClient exposureNotificationClient;

	public static synchronized GoogleExposureClient getInstance(Context context) {
		if (instance == null) {
			instance = new GoogleExposureClient(context.getApplicationContext());
		}
		return instance;
	}

	public static GoogleExposureClient wrapTestClient(ExposureNotificationClient testClient) {
		instance = new GoogleExposureClient(testClient);
		return instance;
	}

	private GoogleExposureClient(Context context) {
		exposureNotificationClient = Nearby.getExposureNotificationClient(context);
	}

	private GoogleExposureClient(ExposureNotificationClient fakeClient) {
		exposureNotificationClient = fakeClient;
	}

	public void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Consumer<Exception> errorCallback) {
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

	public void stop() {
		exposureNotificationClient.stop()
				.addOnSuccessListener(nothing -> Logger.i(TAG, "stop: stopped successfully"))
				.addOnFailureListener(e -> Logger.e(TAG, "stop", e));
	}

	public Task<Boolean> isEnabled() {
		return exposureNotificationClient.isEnabled();
	}

	public void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
			OnSuccessListener<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback) {
		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(temporaryExposureKeys -> {
					Logger.d(TAG, "getTemporaryExposureKeyHistory: success");
					successCallback.onSuccess(temporaryExposureKeys);
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

	public List<TemporaryExposureKey> getTemporaryExposureKeyHistorySynchronous() throws Exception {

		CountDownLatch countDownLatch = new CountDownLatch(1);
		Object[] results = new Object[] { null };

		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(list -> {
					results[0] = list;
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					results[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("getTemporaryExposureKeyHistory() did not result in success or failure in time.");
		}

		if (results[0] instanceof Exception) {
			throw (Exception) results[0];
		} else if (results[0] instanceof List) {
			return (List<TemporaryExposureKey>) results[0];
		} else {
			throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
		}
	}

	public void provideDiagnosisKeys(List<File> keys) throws Exception {
		if (keys == null || keys.isEmpty()) {
			return;
		}

		CountDownLatch countDownLatch = new CountDownLatch(1);
		Exception[] exceptions = new Exception[] { null };
		exposureNotificationClient.provideDiagnosisKeys(new DiagnosisKeyFileProvider(keys))
				.addOnSuccessListener(nothing -> {
					Logger.d(TAG, "provideDiagnosisKeys: inserted keys successfully");
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					Logger.e(TAG, "provideDiagnosisKeys", e);
					exceptions[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("provideDiagnosisKeys() did not result in success or failure in time.");
		}

		if (exceptions[0] != null) {
			throw exceptions[0];
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void provideDiagnosisKeys(List<File> keys, ExposureConfiguration exposureConfiguration, String token) throws Exception {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}

		CountDownLatch countDownLatch = new CountDownLatch(1);
		Exception[] exceptions = new Exception[] { null };
		exposureNotificationClient.provideDiagnosisKeys(keys, exposureConfiguration, token)
				.addOnSuccessListener(nothing -> {
					Logger.d(TAG, "provideDiagnosisKeys: inserted keys successfully for token " + token);
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					Logger.e(TAG, "provideDiagnosisKeys for token " + token, e);
					exceptions[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("provideDiagnosisKeys() did not result in success or failure in time.");
		}

		if (exceptions[0] != null) {
			throw exceptions[0];
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public ExposureSummary getExposureSummary(String token) throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Object[] results = new Object[] { null };

		exposureNotificationClient.getExposureSummary(token)
				.addOnSuccessListener(summary -> {
					results[0] = summary;
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					results[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("getExposureSummary() did not result in success or failure in time.");
		}

		if (results[0] instanceof Exception) {
			throw (Exception) results[0];
		} else if (results[0] instanceof ExposureSummary) {
			return (ExposureSummary) results[0];
		} else {
			throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
		}
	}

	public List<ExposureWindow> getExposureWindows() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Object[] results = new Object[] { null };

		exposureNotificationClient.getExposureWindows()
				.addOnSuccessListener(list -> {
					results[0] = list;
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					results[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("getExposureWindows() did not result in success or failure in time.");
		}

		if (results[0] instanceof Exception) {
			throw (Exception) results[0];
		} else if (results[0] instanceof List) {
			return (List<ExposureWindow>) results[0];
		} else {
			throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
		}
	}

	public void getVersion(OnSuccessListener<Long> onSuccessListener, OnFailureListener onFailureListener) {
		exposureNotificationClient.getVersion()

				.addOnSuccessListener(onSuccessListener)
				.addOnFailureListener(onFailureListener);
	}

	public Integer getCalibrationConfidence() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Object[] results = new Object[] { null };

		exposureNotificationClient.getCalibrationConfidence()
				.addOnSuccessListener(confidence -> {
					results[0] = confidence;
					countDownLatch.countDown();
				})
				.addOnFailureListener(e -> {
					results[0] = e;
					countDownLatch.countDown();
				});

		boolean noTimeout = countDownLatch.await(TIMEOUT_EXPOSURE_CLIENT_CALL_MS, TimeUnit.MILLISECONDS);
		if (!noTimeout) {
			throw new IllegalStateException("getCalibrationConfidence() did not result in success or failure in time.");
		}
		
		if (results[0] instanceof Exception) {
			throw (Exception) results[0];
		} else if (results[0] instanceof Integer) {
			return (Integer) results[0];
		} else {
			throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
		}
	}

}
