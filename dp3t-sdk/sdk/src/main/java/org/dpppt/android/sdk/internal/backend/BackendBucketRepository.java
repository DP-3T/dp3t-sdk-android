/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.PublicKey;

import com.google.protobuf.InvalidProtocolBufferException;

import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.backend.SignatureVerificationInterceptor;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.protobuf.ProtoConverterFactory;

public class BackendBucketRepository implements Repository {

	public static long BATCH_LENGTH = 2 * 60 * 60 * 1000L;

	private BucketService bucketService;

	public BackendBucketRepository(@NonNull Context context, @NonNull String bucketBaseUrl, @NonNull PublicKey publicKey) {
		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(bucketBaseUrl)
				.client(getClientBuilder(context)
						.addInterceptor(new TimingVerificationInterceptor())
						.addInterceptor(new SignatureVerificationInterceptor(publicKey))
						.build())
				.addConverterFactory(ProtoConverterFactory.create())
				.build();

		bucketService = bucketRetrofit.create(BucketService.class);
	}

	public Exposed.ProtoExposedList getExposees(long batchReleaseTime)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SignatureException {
		Response<Exposed.ProtoExposedList> response;
		try {
			response = bucketService.getExposees(batchReleaseTime).execute();
		} catch (RuntimeException re) {
			if (re.getCause() instanceof InvalidProtocolBufferException) {
				// unwrap protobuf exception
				throw (InvalidProtocolBufferException) re.getCause();
			} else {
				throw re;
			}
		}
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		} else {
			throw new StatusCodeException(response.raw());
		}
	}

}
