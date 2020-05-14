package org.dpppt.android.sdk.internal.nearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.ExposureDayStorage;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;

public class ExposureNotificationBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ENBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED.equals(action)) {
			Logger.i(TAG, "received update for " + intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN));
			ExposureSummary exposureSummary = intent.getParcelableExtra(ExposureNotificationClient.EXTRA_EXPOSURE_SUMMARY);

			if (exposureSummary.getAttenuationDurationsInMinutes()[0] >
					AppConfigManager.getInstance(context).getMinDurationForExposure()) {
				DayDate dayOfExposure = new DayDate().subtractDays(exposureSummary.getDaysSinceLastExposure());
				ExposureDay exposureDay = new ExposureDay(-1, dayOfExposure, System.currentTimeMillis());
				ExposureDayStorage.getInstance(context).addExposureDay(context, exposureDay);
			}
		} else if (ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS.equals(action)) {
			//TODO handle settings change
		}
	}

}
