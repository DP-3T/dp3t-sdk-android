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

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.internal.backend.models.GaenRequest
import org.dpppt.android.sdk.models.ExposeeAuthMethod
import org.dpppt.android.sdk.models.ExposeeAuthMethodAuthorization
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class BackendReportRepository(context: Context, reportBaseUrl: String) : Repository {

	private val reportService: ReportService

	init {
		val reportRetrofit = Retrofit.Builder()
			.baseUrl(reportBaseUrl)
			.client(getClientBuilder(context).build())
			.addConverterFactory(GsonConverterFactory.create())
			.build()

		reportService = reportRetrofit.create(ReportService::class.java)
	}

	fun addGaenExposeeAsync(
		exposeeRequest: GaenRequest,
		exposeeAuthMethod: ExposeeAuthMethod?,
		responseCallback: ResponseCallback<String?>
	) {
		GlobalScope.launch {
			try {
				val response = addGaenExposee(exposeeRequest, exposeeAuthMethod)
				withContext(Dispatchers.Main) {
					responseCallback.onSuccess(response)
				}
			} catch (throwable: Throwable) {
				withContext(Dispatchers.Main) {
					responseCallback.onError(throwable)
				}
			}
		}
	}

	@Throws(StatusCodeException::class, IOException::class)
	suspend fun addGaenExposee(
		exposeeRequest: GaenRequest,
		exposeeAuthMethod: ExposeeAuthMethod?
	): String? = withContext(Dispatchers.IO) {
		val authorizationHeader = if (exposeeAuthMethod is ExposeeAuthMethodAuthorization) exposeeAuthMethod.authorization else null

		val response = reportService.addGaenExposee(exposeeRequest, authorizationHeader)
		if (response.isSuccessful) {
			return@withContext response.headers()["Authorization"]
		} else {
			throw StatusCodeException(response.raw(), response.errorBody())
		}
	}
}