/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethodAuthorization;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;

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

	public void addExposee(@NonNull ExposeeRequest exposeeRequest, ExposeeAuthMethod exposeeAuthMethod,
			@NonNull ResponseCallback<Void> responseCallback) {
		String authorizationHeader = exposeeAuthMethod instanceof ExposeeAuthMethodAuthorization
									 ? ((ExposeeAuthMethodAuthorization) exposeeAuthMethod).getAuthorization()
									 : null;
		reportService.addExposee(exposeeRequest, authorizationHeader).enqueue(new Callback<Void>() {
			@Override
			public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
				if (response.isSuccessful()) {
					responseCallback.onSuccess(null);
				} else {
					onFailure(call, new StatusCodeException(response.raw()));
				}
			}

			@Override
			public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
				responseCallback.onError(throwable);
			}
		});
	}

}
