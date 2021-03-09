package org.dpppt.android.sdk

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.internal.AppConfigManager
import org.dpppt.android.sdk.internal.backend.StatusCodeException
import org.dpppt.android.sdk.internal.backend.models.GaenRequest
import org.dpppt.android.sdk.internal.history.HistoryDatabase
import org.dpppt.android.sdk.internal.history.HistoryEntry
import org.dpppt.android.sdk.internal.history.HistoryEntryType
import org.dpppt.android.sdk.internal.logger.Logger
import org.dpppt.android.sdk.models.ExposeeAuthMethod
import org.dpppt.android.sdk.util.DateUtil
import java.io.IOException
import java.util.*

object DP3TKotlin : DP3T() {

	private const val TAG = "DP3TKotlin Interface"

	suspend fun sendFakeInfectedRequestAsync(
		context: Context,
		exposeeAuthMethod: ExposeeAuthMethod,
		successCallback: Runnable?,
		errorCallback: Runnable?
	) = withContext(Dispatchers.IO) {
		checkInit()

		val appConfigManager = AppConfigManager.getInstance(context)
		val withFederationGateway = appConfigManager.withFederationGateway

		val delayedKeyDate = DateUtil.getCurrentRollingStartNumber()
		val exposeeListRequest = GaenRequest(ArrayList(), delayedKeyDate, withFederationGateway)
		exposeeListRequest.isFake = 1

		val devHistory = appConfigManager.devHistory
		try {
			appConfigManager.getBackendReportRepository(context).addGaenExposee(exposeeListRequest, exposeeAuthMethod)
			Logger.d(TAG, "successfully sent fake request")
			if (devHistory) {
				val historyDatabase = HistoryDatabase.getInstance(context)
				historyDatabase.addEntry(
					HistoryEntry(
						HistoryEntryType.FAKE_REQUEST, null, true,
						System.currentTimeMillis()
					)
				)
			}

			withContext(Dispatchers.Main) {
				successCallback?.run()
			}
		} catch (e: Exception) {
			Logger.d(TAG, "failed to send fake request: " + e.localizedMessage)
			if (devHistory) {
				val historyDatabase = HistoryDatabase.getInstance(context)
				val status = when (e) {
					is StatusCodeException -> e.code.toString()
					is IOException -> "NETW"
					else -> "SYST"
				}
				historyDatabase.addEntry(HistoryEntry(HistoryEntryType.FAKE_REQUEST, status, false, System.currentTimeMillis()))
			}

			withContext(Dispatchers.Main) {
				errorCallback?.run()
			}
		}
	}

}