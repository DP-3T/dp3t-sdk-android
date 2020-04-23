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
import org.dpppt.android.sdk.internal.util.DayDate;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BackendBucketRepository implements Repository {

	private BucketService bucketService;

	public BackendBucketRepository(@NonNull Context context, @NonNull String bucketBaseUrl) {
		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(bucketBaseUrl)
				.client(getClientBuilder(context).build())
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		bucketService = bucketRetrofit.create(BucketService.class);
	}

	public CachedResult<ExposedList> getExposees(@NonNull DayDate dayDate) throws IOException, ResponseException {
		Response<ExposedList> response = bucketService.getExposees(dayDate.formatAsString()).execute();
		if (response.isSuccessful()) {
			return new CachedResult<>(response.body(), response.raw().networkResponse() == null);
		}
		throw new ResponseException(response.raw());
	}

}
