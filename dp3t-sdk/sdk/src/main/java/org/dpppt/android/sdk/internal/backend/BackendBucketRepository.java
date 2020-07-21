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

import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.backend.SignatureVerificationInterceptor;
import org.dpppt.android.sdk.models.DayDate;

import java.io.IOException;
import java.security.PublicKey;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BackendBucketRepository implements Repository {

	private BucketService bucketService;

	public BackendBucketRepository(@NonNull Context context, @NonNull String bucketBaseUrl, @NonNull PublicKey publicKey) {

		OkHttpClient.Builder clientBuilder = getClientBuilder(context)
				.addInterceptor(new TimingVerificationInterceptor());
		if (publicKey != null) {
			clientBuilder.addInterceptor(new SignatureVerificationInterceptor(publicKey));
		}

		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(bucketBaseUrl)
				.client(clientBuilder.build())
				.build();

		bucketService = bucketRetrofit.create(BucketService.class);
	}

	public Response<ResponseBody> getGaenExposees(DayDate keyDate, Long lastLoadedTime)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SignatureException {
		Response<ResponseBody> response;
		response = bucketService.getGaenExposees(keyDate.getStartOfDayTimestamp(), lastLoadedTime).execute();
		if (response.isSuccessful()) {
			return response;
		} else {
			throw new StatusCodeException(response.raw(), response.errorBody());
		}
	}

}
