package org.dpppt.android.sdk.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.dpppt.android.sdk.internal.logger.Logger;

public class LocationServiceBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "LocationServiceBR";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO: check if this works without permission
		if (LocationManager.MODE_CHANGED_ACTION.equals(intent.getAction())) {
			Logger.w(TAG, LocationManager.MODE_CHANGED_ACTION);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
		}
	}

}
