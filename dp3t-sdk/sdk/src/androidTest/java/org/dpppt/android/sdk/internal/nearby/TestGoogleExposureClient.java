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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.tasks.Task;

public class TestGoogleExposureClient implements ExposureNotificationClient {

	int provideDiagnosisKeysCounter = 0;

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
		return new DummyTask<>(new ArrayList<>());
	}

	@Override
	public Task<Void> provideDiagnosisKeys(List<File> list, ExposureConfiguration exposureConfiguration, String s) {
		provideDiagnosisKeysCounter++;
		return new DummyTask<>(null);
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
	public ApiKey<Api.ApiOptions.NoOptions> getApiKey() {
		return null;
	}

	public int getProvideDiagnosisKeysCounter() {
		return provideDiagnosisKeysCounter;
	}

}
