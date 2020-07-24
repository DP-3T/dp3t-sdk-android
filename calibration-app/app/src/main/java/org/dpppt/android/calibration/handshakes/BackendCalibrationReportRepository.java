/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration.handshakes;

import android.content.Context;

import org.dpppt.android.calibration.MainApplication;
import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.backend.Repository;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BackendCalibrationReportRepository implements Repository {

	private CalibrationReportService reportService;

	public BackendCalibrationReportRepository(@NonNull Context context) {
		Retrofit reportRetrofit = new Retrofit.Builder()
				.baseUrl(MainApplication.BASE_URL)
				.client(getClientBuilder(context).build())
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		reportService = reportRetrofit.create(CalibrationReportService.class);
	}

	public void addGaenExposee(@NonNull GaenRequest exposeeRequest, String userName,
			@NonNull ResponseCallback<Void> responseCallback) {
		reportService.addGaenExposee(exposeeRequest, userName).enqueue(new Callback<Void>() {
			@Override
			public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
				if (response.isSuccessful()) {
					responseCallback.onSuccess(null);
				} else {
					onFailure(call, new StatusCodeException(response.raw(), response.errorBody()));
				}
			}

			@Override
			public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
				responseCallback.onError(throwable);
			}
		});
	}

}
