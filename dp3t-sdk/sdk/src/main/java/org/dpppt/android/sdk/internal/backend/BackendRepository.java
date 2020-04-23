/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.IOException;

import org.dpppt.android.sdk.internal.backend.models.CachedResult;
import org.dpppt.android.sdk.internal.backend.models.ExposedList;
import org.dpppt.android.sdk.internal.backend.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.internal.backend.models.ExposeeAuthMethodAuthorization;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.util.DayDate;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BackendRepository implements Repository {

	private BackendService backendService;

	public BackendRepository(@NonNull Context context, @NonNull String backendBaseUrl) {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(backendBaseUrl)
				.client(getClientBuilder(context).build())
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		backendService = retrofit.create(BackendService.class);
	}

	public CachedResult<ExposedList> getExposees(@NonNull DayDate dayDate) throws IOException, ResponseException {
		Response<ExposedList> response = backendService.getExposees(dayDate.formatAsString()).execute();
		if (response.isSuccessful()) {
			return new CachedResult<>(response.body(), response.raw().networkResponse() == null);
		}
		throw new ResponseException(response.raw());
	}

	public void addExposee(@NonNull ExposeeRequest exposeeRequest, ExposeeAuthMethod exposeeAuthMethod,
			@NonNull CallbackListener<Void> callbackListener) {
		String authorizationHeader =
				exposeeAuthMethod instanceof ExposeeAuthMethodAuthorization ? ((ExposeeAuthMethodAuthorization) exposeeAuthMethod)
						.getAuthorization() : null;
		backendService.addExposee(exposeeRequest, authorizationHeader).enqueue(new Callback<Void>() {
			@Override
			public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
				if (response.isSuccessful()) {
					callbackListener.onSuccess(null);
				} else {
					onFailure(call, new ResponseException(response.raw()));
				}
			}

			@Override
			public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
				callbackListener.onError(throwable);
			}
		});
	}

}
