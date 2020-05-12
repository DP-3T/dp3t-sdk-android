package org.dpppt.android.sdk.internal.nearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;

import org.dpppt.android.sdk.internal.logger.Logger;

public class ExposureNotificationBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ENBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED.equals(action)) {
			Logger.i(TAG, action);
			// TODO: we've been exposed
		} /*else if (ExposureNotificationClient.ACTION_REQUEST_DIAGNOSIS_KEYS.equals(action)) {
			// TODO: load exposed list from backend
			// TODO: ExposureClient#provideDiagnosisKeys()
			Logger.i(TAG, action);
		}*/
	}

}
