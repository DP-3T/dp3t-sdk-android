/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.IOException;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.backend.models.GaenSecondDay;
import org.dpppt.android.sdk.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.models.ExposeeAuthMethodAuthorization;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BackendReportRepository implements Repository {

	private ReportService reportService;

	public BackendReportRepository(@NonNull Context context, String reportBaseUrl) {
		Retrofit reportRetrofit = new Retrofit.Builder()
				.baseUrl(reportBaseUrl)
				.client(getClientBuilder(context).build())
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		reportService = reportRetrofit.create(ReportService.class);
	}

	public void addGaenExposee(@NonNull GaenRequest exposeeRequest, ExposeeAuthMethod exposeeAuthMethod,
			@NonNull ResponseCallback<String> responseCallback) {
		String authorizationHeader = exposeeAuthMethod instanceof ExposeeAuthMethodAuthorization
									 ? ((ExposeeAuthMethodAuthorization) exposeeAuthMethod).getAuthorization()
									 : null;
		reportService.addGaenExposee(exposeeRequest, authorizationHeader).enqueue(new Callback<Void>() {
			@Override
			public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
				if (response.isSuccessful()) {
					responseCallback.onSuccess(response.headers().get("Authorization"));
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

	public void addPendingGaenKey(GaenKey gaenKey, String token) throws IOException, StatusCodeException {
		Response<Void> response = reportService.addPendingGaenKey(new GaenSecondDay(gaenKey), token).execute();
		if (!response.isSuccessful()) {
			throw new StatusCodeException(response.raw(), response.errorBody());
		}
	}

}
