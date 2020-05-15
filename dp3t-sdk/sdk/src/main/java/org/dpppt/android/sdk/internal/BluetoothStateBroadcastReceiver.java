/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
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
		if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
			return;

		int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
		if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
			Logger.w(TAG, BluetoothAdapter.ACTION_STATE_CHANGED);
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
