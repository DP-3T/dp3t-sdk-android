/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.backend.proto.Exposed;

import retrofit2.Call;
import retrofit2.http.*;

interface BackendService {

	@Headers("Accept: application/x-protobuf")
	@GET("v1/exposed/{batchReleaseTime}")
	Call<Exposed.ProtoExposedList> getExposees(@Path("batchReleaseTime") long batchReleaseTime);

	@Headers("Accept: application/json")
	@POST("v1/exposed")
	Call<Void> addExposee(@Body ExposeeRequest exposeeRequest, @Header("Authorization") String authorizationHeader);

}
