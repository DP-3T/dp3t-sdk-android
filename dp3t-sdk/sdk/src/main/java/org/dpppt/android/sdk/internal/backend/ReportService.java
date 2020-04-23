package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

interface ReportService {

	@Headers("Accept: application/json")
	@POST("v1/exposed")
	Call<Void> addExposee(@Body ExposeeRequest exposeeRequest, @Header("Authorization") String authorizationHeader);

}
