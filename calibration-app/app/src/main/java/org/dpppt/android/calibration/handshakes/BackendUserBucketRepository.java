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
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.backend.Repository;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BackendUserBucketRepository implements Repository {

	public static long BATCH_LENGTH = 24 * 60 * 60 * 1000L;

	private UserBucketService userBucketService;

	public BackendUserBucketRepository(@NonNull Context context) {
		Retrofit bucketRetrofit = new Retrofit.Builder()
				.baseUrl(MainApplication.BASE_URL)
				.client(getClientBuilder(context)
						.build())
				.build();

		userBucketService = bucketRetrofit.create(UserBucketService.class);
	}

	public ResponseBody getGaenExposees(long batchReleaseTime)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SignatureException {
		Response<ResponseBody> response;
		response = userBucketService.getGaenExposees(batchReleaseTime).execute();
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		} else {
			throw new StatusCodeException(response.raw(), response.errorBody());
		}
	}

}
