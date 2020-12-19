/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.platformapi;

import android.app.Activity;
import android.content.Context;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.ExposureWindow;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.internal.nearby.GoogleExposureNotificationWrapper;

public abstract class PlatformAPIWrapper {

	private static PlatformAPIWrapper instance;

	public static synchronized PlatformAPIWrapper getInstance(Context context) {
		if (instance == null) {
			instance = new GoogleExposureNotificationWrapper(context.getApplicationContext());
		}
		return instance;
	}

	public static PlatformAPIWrapper wrapTestClient(PlatformAPIWrapper testClient) {
		instance = testClient;
		return instance;
	}

	public abstract void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Runnable cancelledCallback,
			Consumer<Exception> errorCallback);

	public abstract void stop();

	public abstract void isEnabled(Consumer<Boolean> successCallback, Consumer<Exception> errorCallback);

	public abstract void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
			Consumer<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback);

	public abstract List<TemporaryExposureKey> getTemporaryExposureKeyHistorySynchronous() throws Exception;

	public abstract void provideDiagnosisKeys(List<File> keys) throws Exception;

	/**
	 * @deprecated
	 */
	@Deprecated
	public abstract void provideDiagnosisKeys(List<File> keys, ExposureConfiguration exposureConfiguration, String token)
			throws Exception;

	/**
	 * @deprecated
	 */
	@Deprecated
	public abstract ExposureSummary getExposureSummary(String token) throws Exception;

	public abstract List<ExposureWindow> getExposureWindows() throws Exception;

	public abstract void getVersion(Consumer<Long> onSuccessListener, Consumer<Exception> onFailureListener);

	public abstract Integer getCalibrationConfidence() throws Exception;

}
