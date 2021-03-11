/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby

import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import androidx.core.util.Consumer
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.dpppt.android.sdk.internal.logger.Logger
import java.io.File

class GoogleExposureClient {

	companion object {
		private const val TAG = "GoogleExposureClient"

		private var instance: GoogleExposureClient? = null

		@JvmStatic
		@Synchronized
		fun getInstance(context: Context): GoogleExposureClient {
			return instance ?: GoogleExposureClient(context.applicationContext).also { instance = it }
		}

		@JvmStatic
		fun wrapTestClient(testClient: ExposureNotificationClient): GoogleExposureClient {
			return GoogleExposureClient(testClient).also { instance = it }
		}
	}

	private val exposureNotificationClient: ExposureNotificationClient

	private constructor(context: Context) {
		exposureNotificationClient = Nearby.getExposureNotificationClient(context)
	}

	private constructor(fakeClient: ExposureNotificationClient) {
		exposureNotificationClient = fakeClient
	}

	fun start(activity: Activity, resolutionRequestCode: Int, successCallback: Runnable, errorCallback: Consumer<Exception>) {
		exposureNotificationClient.start()
			.addOnSuccessListener { nothing: Void? ->
				Logger.i(TAG, "start: started successfully")
				successCallback.run()
			}
			.addOnFailureListener { e: Exception ->
				if (e is ApiException) {
					if (e.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
						try {
							Logger.i(TAG, "start: resolution required")
							e.status.startResolutionForResult(activity, resolutionRequestCode)
							return@addOnFailureListener
						} catch (e2: SendIntentException) {
							Logger.e(TAG, "start: error calling startResolutionForResult()")
						}
					}
				}
				Logger.e(TAG, "start", e)
				errorCallback.accept(e)
			}
	}

	fun stop() {
		exposureNotificationClient.stop()
			.addOnSuccessListener { Logger.i(TAG, "stop: stopped successfully") }
			.addOnFailureListener { e: Exception? -> Logger.e(TAG, "stop", e) }
	}

	val isEnabled: Task<Boolean>
		get() = exposureNotificationClient.isEnabled

	fun getTemporaryExposureKeyHistory(
		activity: Activity, resolutionRequestCode: Int,
		successCallback: OnSuccessListener<List<TemporaryExposureKey?>?>, errorCallback: Consumer<Exception>
	) {
		exposureNotificationClient.temporaryExposureKeyHistory
			.addOnSuccessListener { temporaryExposureKeys: List<TemporaryExposureKey?>? ->
				Logger.d(TAG, "getTemporaryExposureKeyHistory: success")
				successCallback.onSuccess(temporaryExposureKeys)
			}
			.addOnFailureListener { e: Exception? ->
				if (e is ApiException) {
					if (e.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
						try {
							Logger.i(TAG, "getTemporaryExposureKeyHistory: resolution required")
							e.status.startResolutionForResult(activity, resolutionRequestCode)
							return@addOnFailureListener
						} catch (e2: SendIntentException) {
							Logger.e(TAG, "getTemporaryExposureKeyHistory: error calling startResolutionForResult()")
						}
					}
				}
				Logger.e(TAG, "getTemporaryExposureKeyHistory", e)
				errorCallback.accept(e)
			}
	}

	@Throws(Exception::class)
	suspend fun getTemporaryExposureKeyHistorySynchronous(): List<TemporaryExposureKey> {
		return exposureNotificationClient.temporaryExposureKeyHistory.await()
	}

	@Throws(Exception::class)
	fun provideDiagnosisKeysBlocking(keys: List<File>?) = runBlocking {
		provideDiagnosisKeys(keys)
	}

	@Throws(Exception::class)
	suspend fun provideDiagnosisKeys(keys: List<File>?) {
		if (keys == null || keys.isEmpty()) {
			return
		}
		exposureNotificationClient.provideDiagnosisKeys(DiagnosisKeyFileProvider(keys)).await()
	}

	@Deprecated("")
	@Throws(Exception::class)
	fun provideDiagnosisKeysBlocking(keys: List<File>?, exposureConfiguration: ExposureConfiguration, token: String) = runBlocking {
		provideDiagnosisKeys(keys, exposureConfiguration, token)
	}

	@Deprecated("")
	@Throws(Exception::class)
	suspend fun provideDiagnosisKeys(keys: List<File>?, exposureConfiguration: ExposureConfiguration, token: String) {
		if (keys == null || keys.isEmpty()) {
			return
		}
		exposureNotificationClient.provideDiagnosisKeys(keys, exposureConfiguration, token).await()
	}

	@Deprecated("")
	@Throws(Exception::class)
	fun getExposureSummaryBlocking(token: String): ExposureSummary = runBlocking {
		getExposureSummary(token)
	}

	@Deprecated("")
	@Throws(Exception::class)
	suspend fun getExposureSummary(token: String): ExposureSummary {
		return exposureNotificationClient.getExposureSummary(token).await()
	}

	@Throws(Exception::class)
	fun getExposureWindowsBlocking(): List<ExposureWindow> = runBlocking {
		getExposureWindows()
	}

	@Throws(Exception::class)
	suspend fun getExposureWindows(): List<ExposureWindow> {
		return exposureNotificationClient.exposureWindows.await()
	}

	fun getVersion(onSuccessListener: OnSuccessListener<Long>, onFailureListener: OnFailureListener) {
		exposureNotificationClient.version
			.addOnSuccessListener(onSuccessListener)
			.addOnFailureListener(onFailureListener)
	}

	@Throws(Exception::class)
	fun getCalibrationConfidenceBlocking(): Int = runBlocking {
		exposureNotificationClient.calibrationConfidence.await()
	}

}