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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.backend.SignatureException
import org.dpppt.android.sdk.backend.SignatureVerificationInterceptor
import org.dpppt.android.sdk.internal.backend.models.GaenRequest
import org.dpppt.android.sdk.models.ExposeeAuthMethod
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.security.PublicKey

class BackendBucketRepository(context: Context, bucketBaseUrl: String, publicKey: PublicKey?) : Repository {

	private val bucketService: BucketService

	init {
		val clientBuilder = getClientBuilder(context).addInterceptor(TimingVerificationInterceptor())

		if (publicKey != null) {
			clientBuilder.addInterceptor(SignatureVerificationInterceptor(publicKey))
		}

		val bucketRetrofit = Retrofit.Builder()
			.baseUrl(bucketBaseUrl)
			.client(clientBuilder.build())
			.build()

		bucketService = bucketRetrofit.create(BucketService::class.java)
	}

	@Throws(IOException::class, StatusCodeException::class, ServerTimeOffsetException::class, SignatureException::class)
	fun getGaenExposeesBlocking(
		lastKeyBundleTag: String?,
		withFederationGateway: Boolean?
	) = runBlocking {
		getGaenExposees(lastKeyBundleTag, withFederationGateway)
	}

	@Throws(IOException::class, StatusCodeException::class, ServerTimeOffsetException::class, SignatureException::class)
	suspend fun getGaenExposees(
		lastKeyBundleTag: String?,
		withFederationGateway: Boolean?
	): Response<ResponseBody> {
			val response = bucketService.getGaenExposees(lastKeyBundleTag, withFederationGateway)
			return if (response.isSuccessful) {
				response
			} else {
				throw StatusCodeException(response.raw(), response.errorBody())
			}
	}
}