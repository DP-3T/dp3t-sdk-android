/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;

public class BackendRepository implements Repository {

	public static final long BATCH_LENGTH = 2 * 60 * 60 * 1000L;

	private BackendService backendService;

	public BackendRepository(@NonNull Context context, @NonNull String backendBaseUrl) {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(backendBaseUrl)
				.client(getClientBuilder(context).build())
				.addConverterFactory(ProtoConverterFactory.create())
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		backendService = retrofit.create(BackendService.class);
	}

	public Exposed.ProtoExposedList getExposees(long batchReleaseTime) throws IOException, ResponseException {
		if (batchReleaseTime % BATCH_LENGTH != 0) {
			throw new IllegalArgumentException("invalid batchReleaseTime: " + batchReleaseTime);
		}

		Response<Exposed.ProtoExposedList> response = backendService.getExposees(batchReleaseTime).execute();
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		} else {
			throw new ResponseException(response.raw());
		}
	}

	public void addExposee(@NonNull ExposeeRequest exposeeRequest, @Nullable String authorizationHeader,
			@NonNull CallbackListener<Void> callbackListener) {
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
