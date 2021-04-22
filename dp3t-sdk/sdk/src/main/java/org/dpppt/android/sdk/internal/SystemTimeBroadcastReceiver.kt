package org.dpppt.android.sdk.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dpppt.android.sdk.TracingStatus.ErrorState
import org.dpppt.android.sdk.internal.SyncWorker.SyncImpl
import org.dpppt.android.sdk.internal.backend.SyncErrorState
import org.dpppt.android.sdk.internal.logger.Logger

class SystemTimeBroadcastReceiver : BroadcastReceiver() {

	companion object {
		private const val TAG = "SystemTimeBR"
	}

	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_TIME_CHANGED != intent.action && Intent.ACTION_DATE_CHANGED != intent.action) return

		Logger.w(TAG, intent.action)

		val syncError = SyncErrorState.getInstance().getSyncError(context)
		val appConfigManager = AppConfigManager.getInstance(context)

		if (syncError == ErrorState.SYNC_ERROR_TIMING && appConfigManager.lastSyncCallTime != -1L) {
			Logger.w(TAG, "invalidating sync")

			appConfigManager.lastSyncCallTime = -1
			appConfigManager.lastSyncDate = -1

			GlobalScope.launch(Dispatchers.IO) {
				try {
					SyncImpl(context).doSync()
				} catch (ignored: Exception) {
					// has been handled upstream
				}
			}
		}
	}

}