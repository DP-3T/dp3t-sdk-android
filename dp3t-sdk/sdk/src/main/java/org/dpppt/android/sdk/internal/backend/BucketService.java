/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

interface BucketService {

	@Headers("Accept: application/x-protobuf")
	@GET("v1/exposed/{batchReleaseTime}")
	Call<Exposed.ProtoExposedList> getExposees(@Path("batchReleaseTime") long batchReleaseTime);

}
