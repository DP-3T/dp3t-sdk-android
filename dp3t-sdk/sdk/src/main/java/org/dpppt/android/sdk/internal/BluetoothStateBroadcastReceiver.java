package org.dpppt.android.sdk.internal;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.internal.logger.Logger;

public class BluetoothStateBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "BluetoothStateBR";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
			if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
				Logger.w(TAG, BluetoothAdapter.ACTION_STATE_CHANGED);
				BroadcastHelper.sendErrorUpdateBroadcast(context);
			}
		}
	}

}
