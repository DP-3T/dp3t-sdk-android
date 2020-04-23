/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.content.Intent;

import org.dpppt.android.sdk.DP3T;

public class BroadcastHelper {

	public static final String ACTION_UPDATE_ERRORS = "org.dpppt.android.sdk.internal.ACTION_UPDATE_ERRORS";

	public static void sendUpdateBroadcast(Context context) {
		Intent intent = new Intent(DP3T.UPDATE_INTENT_ACTION);
		context.sendBroadcast(intent);
	}

	public static void sendErrorUpdateBroadcast(Context context) {
		sendUpdateBroadcast(context);

		Intent intent = new Intent(ACTION_UPDATE_ERRORS);
		context.sendBroadcast(intent);
	}

}
