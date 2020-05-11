package org.dpppt.android.sdk.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.DP3T;

public class TracingErrorsBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "TracingErrorsBR";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (DP3T.ACTION_UPDATE_ERRORS.equals(intent.getAction())) {
			// TODO: invalidateForegroundNotification(); // cross-check with error notifications shown by the system
		}
	}

}
