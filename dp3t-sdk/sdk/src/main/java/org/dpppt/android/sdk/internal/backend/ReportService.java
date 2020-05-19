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

import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.backend.models.GaenSecondDay;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

interface ReportService {

	@Headers("Accept: application/json")
	@POST("v1/gaen/exposed")
	Call<Void> addGaenExposee(@Body GaenRequest exposeeRequest, @Header("Authorization") String authorizationHeader);

	@Headers("Accept: application/json")
	@POST("v1/gaen/exposednextday")
	Call<Void> addPendingGaenKey(@Body GaenSecondDay delayedKey, @Header("Authorization") String authorizationHeader);

}
