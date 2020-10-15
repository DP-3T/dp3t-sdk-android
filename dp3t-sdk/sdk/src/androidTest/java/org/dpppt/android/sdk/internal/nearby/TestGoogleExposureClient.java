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

import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.nearby.exposurenotification.*;
import com.google.android.gms.tasks.Task;

import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.util.DateUtil;

public class TestGoogleExposureClient implements ExposureNotificationClient {

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
	public Task<Void> start() {
		return new DummyTask<>(null);
	}

	@Override
	public Task<Void> stop() {
		return new DummyTask<>(null);
	}

	@Override
	public Task<Boolean> isEnabled() {
		return new DummyTask<>(true);
	}

	@Override
	public Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory() {
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
		return new DummyTask<>(temporaryExposureKeys);
	}

	@Override
	public Task<Void> provideDiagnosisKeys(List<File> list, ExposureConfiguration exposureConfiguration, String token) {
		return provideDiagnosisKeys(new DiagnosisKeyFileProvider(list));
	}

	@Override
	public Task<Void> provideDiagnosisKeys(List<File> list) {
		return provideDiagnosisKeys(list, null, null);
	}

	@Override
	public Task<Void> provideDiagnosisKeys(DiagnosisKeyFileProvider diagnosisKeyFileProvider) {
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

		return new DummyTask<>(null);
	}

	@Override
	public Task<List<ExposureWindow>> getExposureWindows(String s) {
		return new DummyTask<>(params == null ? new ArrayList<>() : params.exposureWindows);
	}

	@Override
	public Task<List<ExposureWindow>> getExposureWindows() {
		return getExposureWindows(null);
	}

	@Override
	public Task<ExposureSummary> getExposureSummary(String s) {
		return new DummyTask<>(new ExposureSummary.ExposureSummaryBuilder().build());
	}

	@Override
	public Task<List<ExposureInformation>> getExposureInformation(String s) {
		return new DummyTask<>(new ArrayList<>());
	}

	@Override
	public Task<Long> getVersion() {
		return new DummyTask<>(17203704005L);
	}

	@Override
	public Task<Integer> getCalibrationConfidence() {
		return null;
	}

	@Override
	public Task<List<DailySummary>> getDailySummaries(DailySummariesConfig dailySummariesConfig) {
		return null;
	}

	@Override
	public Task<Void> setDiagnosisKeysDataMapping(DiagnosisKeysDataMapping diagnosisKeysDataMapping) {
		return null;
	}

	@Override
	public Task<DiagnosisKeysDataMapping> getDiagnosisKeysDataMapping() {
		return null;
	}

	@Override
	public boolean deviceSupportsLocationlessScanning() {
		return false;
	}

	@Override
	public Task<Set<ExposureNotificationStatus>> getStatus() {
		return null;
	}

	@Override
	public Task<PackageConfiguration> getPackageConfiguration() {
		return null;
	}

	@Override
	public ApiKey<Api.ApiOptions.NoOptions> getApiKey() {
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
