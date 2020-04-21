/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

import org.dpppt.android.sdk.internal.logger.Logger;

public class TracingServiceBroadcastReceiver extends BroadcastReceiver {

	public static final String TAG = "TracingServiceBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent i) {
		Logger.d(TAG, "received broadcast to start service");
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		boolean advertising = appConfigManager.isAdvertisingEnabled();
		boolean receiving = appConfigManager.isReceivingEnabled();
		long scanInterval = appConfigManager.getScanInterval();
		long scanDuration = appConfigManager.getScanDuration();
		if (advertising || receiving) {
			Intent intent = new Intent(context, TracingService.class).setAction(i.getAction());
			intent.putExtra(TracingService.EXTRA_ADVERTISE, advertising);
			intent.putExtra(TracingService.EXTRA_RECEIVE, receiving);
			intent.putExtra(TracingService.EXTRA_SCAN_INTERVAL, scanInterval);
			intent.putExtra(TracingService.EXTRA_SCAN_DURATION, scanDuration);
			ContextCompat.startForegroundService(context, intent);
		}
	}

}
