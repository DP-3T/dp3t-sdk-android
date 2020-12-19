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
import android.content.Intent;
import androidx.core.util.Consumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.*;

import org.dpppt.android.sdk.internal.platformapi.PlatformAPIWrapper;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.util.DateUtil;

public class TestGoogleExposureClient extends PlatformAPIWrapper {

	private Context context;
	private int provideDiagnosisKeysCounter = 0;
	private boolean currentDayKeyReleased = false;
	private long time = System.currentTimeMillis();
	private ExposureTestParameters params;

	public TestGoogleExposureClient(Context context) {
		this.context = context;
	}

	public TestGoogleExposureClient(Context context, boolean currentDayKeyReleased) {
		this.context = context;
		this.currentDayKeyReleased = currentDayKeyReleased;
	}

	@Override
	public void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Runnable cancelledCallback,
			Consumer<Exception> errorCallback) {
		successCallback.run();
	}

	@Override
	public void stop() {
		//do nothing
	}

	@Override
	public void isEnabled(Consumer<Boolean> successCallback, Consumer<Exception> errorCallback) {
		successCallback.accept(true);
	}

	@Override
	public void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
			Consumer<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback) {
		try {
			successCallback.accept(getTemporaryExposureKeyHistorySynchronous());
		} catch (Exception e) {
			errorCallback.accept(e);
		}
	}

	@Override
	public List<TemporaryExposureKey> getTemporaryExposureKeyHistorySynchronous() throws Exception {
		ArrayList<TemporaryExposureKey> temporaryExposureKeys = new ArrayList<>();
		for (int i = 1; i < 14; i++) {
			temporaryExposureKeys.add(new TemporaryExposureKey.TemporaryExposureKeyBuilder()
					.setRollingStartIntervalNumber(DateUtil.getRollingStartNumberForDate(new DayDate(time).subtractDays(i)))
					.build());
		}
		if (currentDayKeyReleased) {
			temporaryExposureKeys.add(new TemporaryExposureKey.TemporaryExposureKeyBuilder()
					.setRollingStartIntervalNumber(DateUtil.getRollingStartNumberForDate(new DayDate(time)))
					.build());
		}
		return temporaryExposureKeys;
	}


	@Override
	public void provideDiagnosisKeys(List<File> list, ExposureConfiguration exposureConfiguration, String token) {
		provideDiagnosisKeys(new DiagnosisKeyFileProvider(list));
	}

	@Override
	public void provideDiagnosisKeys(List<File> list) {
		provideDiagnosisKeys(list, null, null);
	}

	public void provideDiagnosisKeys(DiagnosisKeyFileProvider diagnosisKeyFileProvider) {
		provideDiagnosisKeysCounter++;

		while (diagnosisKeyFileProvider.zza()) {
			File file = diagnosisKeyFileProvider.zzb();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				String fileContent = reader.readLine();
				if (fileContent.startsWith("{")) {
					params = Json.fromJson(fileContent, ExposureTestParameters.class);
					Intent intent = new Intent(ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED);
					intent.putExtra(ExposureNotificationClient.EXTRA_TOKEN, ExposureNotificationClient.TOKEN_A);
					new ExposureNotificationBroadcastReceiver().onReceive(context, intent);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<ExposureWindow> getExposureWindows() {
		return params == null ? new ArrayList<>() : params.exposureWindows;
	}

	@Override
	public ExposureSummary getExposureSummary(String s) {
		return new ExposureSummary.ExposureSummaryBuilder().build();
	}

	@Override
	public void getVersion(Consumer<Long> onSuccessListener, Consumer<Exception> onFailureListener) {
		onSuccessListener.accept(17203704005L);
	}

	@Override
	public Integer getCalibrationConfidence() throws Exception {
		return null;
	}

	public int getProvideDiagnosisKeysCounter() {
		return provideDiagnosisKeysCounter;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public static class ExposureTestParameters {

		public List<ExposureWindow> exposureWindows;

	}

}
