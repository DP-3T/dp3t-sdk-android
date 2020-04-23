/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class DeviceID {

	public static String getID(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences("deviceID", Context.MODE_PRIVATE);
		String id = sharedPreferences.getString("id", null);
		if (id == null) {
			id = UUID.randomUUID().toString();
			sharedPreferences.edit().putString("id", id).apply();
		}
		return id;
	}

}
