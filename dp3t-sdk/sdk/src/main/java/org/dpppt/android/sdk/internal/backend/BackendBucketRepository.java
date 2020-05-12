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
import java.security.PublicKey;

import org.dpppt.android.sdk.backend.SignatureException;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BackendBucketRepository implements Repository {

	public static long BATCH_LENGTH = 2 * 60 * 60 * 1000L;

	private BucketService bucketService;

	public BackendBucketRepository(@NonNull Context context, @NonNull String bucketBaseUrl, @NonNull PublicKey publicKey) {
		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(bucketBaseUrl)
				.client(getClientBuilder(context)
						.addInterceptor(new TimingVerificationInterceptor())
						// TODO .addInterceptor(new SignatureVerificationInterceptor(publicKey))
						.build())
				.build();

		bucketService = bucketRetrofit.create(BucketService.class);
	}

	public ResponseBody getGaenExposees(long batchReleaseTime)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SignatureException {
		Response<ResponseBody> response;
		response = bucketService.getGaenExposees(batchReleaseTime).execute();
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		} else {
			throw new StatusCodeException(response.raw());
		}
	}

}
