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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.internal.logger.Logger;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "BootCompletedBR";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) &&
				!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
			return;

		Logger.i(TAG, intent.getAction());

		if (DP3T.isTracingEnabled(context)) {
			SyncWorker.startSyncWorker(context);
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
