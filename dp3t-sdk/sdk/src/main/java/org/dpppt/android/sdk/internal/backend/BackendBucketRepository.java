/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.IOException;

import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.protobuf.ProtoConverterFactory;

public class BackendBucketRepository implements Repository {

	public static final long BATCH_LENGTH = 2 * 60 * 60 * 1000L;

	private BucketService bucketService;

	public BackendBucketRepository(@NonNull Context context, @NonNull String bucketBaseUrl) {
		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(bucketBaseUrl)
				.client(getClientBuilder(context).build())
				.addConverterFactory(ProtoConverterFactory.create())
				.build();

		bucketService = bucketRetrofit.create(BucketService.class);
	}

	public Exposed.ProtoExposedList getExposees(long batchReleaseTime) throws IOException, ResponseException {
		Response<Exposed.ProtoExposedList> response = bucketService.getExposees(batchReleaseTime).execute();
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		} else {
			throw new ResponseException(response.raw());
		}
	}

}
