/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.internal.backend.models.ExposedList;
import org.dpppt.android.sdk.internal.backend.models.Exposee;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface BackendService {

	@GET("v1/exposed/{dayDate}")
	Call<ExposedList> getExposees(@Path("dayDate") String dayDate);

	@POST("v1/exposed")
	Call<Void> addExposee(@Body ExposeeRequest exposeeRequest, @Header("Authorization") String authorizationHeader);

}
