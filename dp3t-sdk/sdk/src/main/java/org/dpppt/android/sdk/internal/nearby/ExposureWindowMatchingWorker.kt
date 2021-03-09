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

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import org.dpppt.android.sdk.internal.AppConfigManager
import org.dpppt.android.sdk.internal.logger.Logger
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage
import org.dpppt.android.sdk.models.DayDate
import org.dpppt.android.sdk.models.ExposureDay
import java.util.*
import kotlin.math.ceil

class ExposureWindowMatchingWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

	companion object {

		const val WORK_TAG = "org.dpppt.android.sdk.internal.nearby.ExposureWindowMatchingWorker"
		private const val TAG = "MatchingWorker"

		@JvmStatic
		fun startMatchingWorker(context: Context) {
			val workManager = WorkManager.getInstance(context)
			workManager.enqueue(OneTimeWorkRequest.Builder(ExposureWindowMatchingWorker::class.java).addTag(WORK_TAG).build())
			Logger.d(TAG, "scheduled MatchingWorker")
		}

		private fun addDaysWhereExposureLimitIsReached(context: Context, exposureWindows: List<ExposureWindow>) {
			val appConfigManager = AppConfigManager.getInstance(context)
			val attenuationDurationsInSecondsForDate = mutableMapOf<DayDate, IntArray>()
			for (exposureWindow in exposureWindows) {
				val windowDate = DayDate(exposureWindow.dateMillisSinceEpoch)
				Logger.d(TAG, "Received ExposureWindow for " + windowDate.formatAsString() + ": " + exposureWindow.toString())
				if (!attenuationDurationsInSecondsForDate.containsKey(windowDate)) {
					attenuationDurationsInSecondsForDate[windowDate] = intArrayOf(0, 0, 0)
				}
				val attenuationDurationsInSeconds = attenuationDurationsInSecondsForDate[windowDate] ?: continue
				for (scanInstance in exposureWindow.scanInstances) {
					if (scanInstance.typicalAttenuationDb < appConfigManager.attenuationThresholdLow) {
						attenuationDurationsInSeconds[0] += scanInstance.secondsSinceLastScan
					} else if (scanInstance.typicalAttenuationDb < appConfigManager.attenuationThresholdMedium) {
						attenuationDurationsInSeconds[1] += scanInstance.secondsSinceLastScan
					} else {
						attenuationDurationsInSeconds[2] += scanInstance.secondsSinceLastScan
					}
				}
			}
			val exposureDays = getExposureDaysFromAttenuationDurations(context, attenuationDurationsInSecondsForDate)
			if (exposureDays.isNotEmpty()) {
				ExposureDayStorage.getInstance(context).addExposureDays(context, exposureDays)
			}
		}

		private fun getExposureDaysFromAttenuationDurations(
			context: Context,
			attenuationDurationsInSecondsForDate: Map<DayDate, IntArray>
		): List<ExposureDay> {
			val exposureDays: MutableList<ExposureDay> = ArrayList()
			val maxAgeForExposure = DayDate().subtractDays(AppConfigManager.getInstance(context).numberOfDaysToConsiderForExposure)
			for ((key, value) in attenuationDurationsInSecondsForDate) {
				if (key.isBefore(maxAgeForExposure)) {
					Logger.d(TAG, "exposure too far in the past on " + key.formatAsString())
					continue
				}
				Logger.d(
					TAG, "Checking exposure limit for " + key.formatAsString() + ": " + value.contentToString()
				)
				val attenuationDurationsInMinutes = convertAttenuationDurationsToMinutes(value)
				if (isExposureLimitReached(context, attenuationDurationsInMinutes)) {
					Logger.d(TAG, "exposure limit reached on " + key.formatAsString())
					val exposureDay = ExposureDay(-1, key, System.currentTimeMillis())
					exposureDays.add(exposureDay)
				} else {
					Logger.d(TAG, "exposure limit not reached on " + key.formatAsString())
				}
			}
			return exposureDays
		}

		@JvmStatic
		fun convertAttenuationDurationsToMinutes(attenuationDurationsInSeconds: IntArray): IntArray {
			val attenuationDurationsInMinutes = IntArray(3)
			for (i in 0..2) {
				attenuationDurationsInMinutes[i] = ceil(attenuationDurationsInSeconds[i] / 60.0).toInt()
			}
			return attenuationDurationsInMinutes
		}

		@JvmStatic
		fun isExposureLimitReached(context: Context, attenuationDurationsInMinutes: IntArray): Boolean {
			val appConfigManager = AppConfigManager.getInstance(context)
			return computeExposureDuration(appConfigManager, attenuationDurationsInMinutes) >=
					appConfigManager.minDurationForExposure
		}

		private fun computeExposureDuration(appConfigManager: AppConfigManager, attenuationDurationsInMinutes: IntArray): Float {
			return attenuationDurationsInMinutes[0] * appConfigManager.attenuationFactorLow +
					attenuationDurationsInMinutes[1] * appConfigManager.attenuationFactorMedium
		}
	}

	override suspend fun doWork(): Result {
		val context = applicationContext
		val exposureWindows: List<ExposureWindow> = try {
			GoogleExposureClient.getInstance(context).getExposureWindows()
		} catch (e: Exception) {
			Logger.e(TAG, "error getting exposureWindows")
			return Result.failure()
		}
		addDaysWhereExposureLimitIsReached(context, exposureWindows)
		return Result.success()
	}

}