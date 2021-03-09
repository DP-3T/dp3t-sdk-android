/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface BucketService {

	@GET("v2/gaen/exposed")
	suspend fun getGaenExposees(
		@Query("lastKeyBundleTag") lastKeyBundleTag: String?,
		@Query("withFederationGateway") withFederationGateway: Boolean?
	): Response<ResponseBody>

}