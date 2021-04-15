/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.dpppt.android.sdk.BuildConfig
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository
import org.dpppt.android.sdk.internal.backend.SyncErrorState
import org.dpppt.android.sdk.internal.history.HistoryDatabase
import org.dpppt.android.sdk.internal.history.HistoryEntry
import org.dpppt.android.sdk.internal.history.HistoryEntryType
import org.dpppt.android.sdk.internal.logger.Logger
import org.dpppt.android.sdk.internal.nearby.GaenStateCache
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient.Companion.getInstance
import java.io.File
import java.security.PublicKey
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

	companion object {

		private const val TAG = "SyncWorker"
		const val WORK_NAME = "org.dpppt.android.sdk.internal.SyncWorker"
		const val WORK_TAG = WORK_NAME
		private const val KEYFILE_PREFIX = "keyfile_"

		const val KEY_BUNDLE_TAG_HEADER = "x-key-bundle-tag"

		private var bucketSignaturePublicKey: PublicKey? = null

		private val isSyncInProgress = AtomicBoolean(false)

		@JvmStatic
		fun startSyncWorker(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val periodicWorkRequest = PeriodicWorkRequest.Builder(SyncWorker::class.java, 120, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.addTag(WORK_TAG)
				.build()
			val workManager = WorkManager.getInstance(context)
			workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest)
			Logger.d(TAG, "scheduled SyncWorker")
		}

		@JvmStatic
		fun stopSyncWorker(context: Context) {
			val workManager = WorkManager.getInstance(context)
			workManager.cancelUniqueWork(WORK_NAME)
		}

		@JvmStatic
		fun setBucketSignaturePublicKey(publicKey: PublicKey?) {
			bucketSignaturePublicKey = publicKey
		}
	}

	override suspend fun doWork(): Result {
		Logger.d(TAG, "start SyncWorker")
		val context = applicationContext
		if (AppConfigManager.getInstance(context).devHistory) {
			HistoryDatabase.getInstance(context)
				.addEntry(HistoryEntry(HistoryEntryType.WORKER_STARTED, "Sync", true, System.currentTimeMillis()))
		}
		try {
			SyncImpl(context).doSync()
		} catch (e: Exception) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.message)
			return Result.failure()
		}
		Logger.d(TAG, "SyncWorker finished with success")
		return Result.success()
	}

	class SyncImpl @JvmOverloads constructor(
		private val context: Context,
		private val currentTime: Long = System.currentTimeMillis()
	) {

		@Throws(Exception::class)
		fun doSyncBlocking() {
			runBlocking {
				doSync()
			}
		}

		@Throws(Exception::class)
		suspend fun doSync() {
			if (!isSyncInProgress.compareAndSet(false, true)) {
				return
			}
			try {
				GaenStateHelper.invalidateGaenAvailability(context)
				GaenStateHelper.invalidateGaenEnabled(context)
				try {
					if (DP3T.isTracingEnabled(context) && java.lang.Boolean.FALSE != GaenStateCache.isGaenEnabled()) {
						val syncWasExecuted = doSyncInternal(context)
						if (!syncWasExecuted) {
							Logger.i(TAG, "sync skipped due to rate limit")
							return
						}
						Logger.i(TAG, "synced")
					}
					SyncErrorState.getInstance().setSyncError(context, null)
					BroadcastHelper.sendUpdateAndErrorBroadcast(context)
				} catch (e: Exception) {
					Logger.e(TAG, "sync", e)
					val syncError = ErrorHelper.getSyncErrorFromException(e, true)
					SyncErrorState.getInstance().setSyncError(context, syncError)
					BroadcastHelper.sendUpdateAndErrorBroadcast(context)
					throw e
				}
			} finally {
				isSyncInProgress.set(false)
			}
		}

		@Throws(Exception::class)
		private suspend fun doSyncInternal(context: Context): Boolean = withContext(Dispatchers.IO) {
			val appConfigManager = AppConfigManager.getInstance(context)
			val appConfig = appConfigManager.appConfig
			val backendBucketRepository = BackendBucketRepository(context, appConfig.bucketBaseUrl, bucketSignaturePublicKey)
			val googleExposureClient = getInstance(context)
			if (appConfigManager.lastSynCallTime <= currentTime - syncInterval) {
				try {
					Logger.d(TAG, "loading exposees")
					val withFederationGateway = appConfigManager.withFederationGateway
					val result =
						backendBucketRepository.getGaenExposees(appConfigManager.lastKeyBundleTag, withFederationGateway)
					if (result.code() != 204) {
						val file = File(context.cacheDir, KEYFILE_PREFIX + appConfigManager.lastKeyBundleTag + ".zip")
						result.body()!!.byteStream().copyTo(file.outputStream())
						val fileList = listOf(file)
						Logger.d(TAG, "provideDiagnosisKeys with size " + file.length())
						appConfigManager.setLastSyncCallTime(currentTime)
						googleExposureClient.provideDiagnosisKeys(fileList)
					} else {
						appConfigManager.setLastSyncCallTime(currentTime)
					}
					appConfigManager.lastKeyBundleTag = result.headers()[KEY_BUNDLE_TAG_HEADER]
					appConfigManager.lastSyncDate = currentTime
					addHistoryEntry(false, false)
				} catch (e: Exception) {
					if (appConfigManager.devHistory) {
						HistoryDatabase.getInstance(context)
							.addEntry(HistoryEntry(HistoryEntryType.SYNC, e.stackTraceToString(), false, currentTime))
					}
					Logger.e(TAG, "error while syncing new keys", e)
					val lastSuccessfulSyncTime = appConfigManager.lastSyncDate
					val isDelayWithinGracePeriod =
						lastSuccessfulSyncTime > currentTime - SyncErrorState.getInstance().syncErrorGracePeriod
					if (isDelayWithinGracePeriod && ErrorHelper.isDelayableSyncError(e)) {
						addHistoryEntry(false, true)
					} else {
						addHistoryEntry(true, false)
						throw e
					}
				}
				cleanupOldKeyFiles(context)
				return@withContext true
			} else {
				return@withContext false
			}
		}

		private val syncInterval: Long
			get() = if (BuildConfig.FLAVOR == "calibration") {
				5 * 60 * 1000L
			} else {
				val syncsPerDay = AppConfigManager.getInstance(context).syncsPerDay
				24 * 60 * 60 * 1000L / syncsPerDay
			}

		private fun addHistoryEntry(instantError: Boolean, delayedError: Boolean) {
			val base = 'A'.toInt()
			val historyStatus: String =
				((base + if (instantError) 1 else 0).toChar().toString() +
						(base + if (delayedError) 1 else 0).toChar().toString() +
						(base + if (!instantError && !delayedError) 1 else 0).toChar().toString())
			HistoryDatabase.getInstance(context).addEntry(
				HistoryEntry(
					HistoryEntryType.SYNC, historyStatus, !instantError && !delayedError,
					System.currentTimeMillis()
				)
			)
		}

		private fun cleanupOldKeyFiles(context: Context) {
			context.cacheDir.listFiles()?.forEach { file ->
				if (file.name.startsWith(KEYFILE_PREFIX)) {
					if (!file.delete()) {
						Logger.w(TAG, "Unable to delete file " + file.name)
					}
				}
			}
		}
	}
}