/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.core.util.Consumer;

import java.io.OutputStream;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.export.ExportDatabaseOpenHelper;
import org.dpppt.android.sdk.internal.export.DeviceHelper;
import org.dpppt.android.sdk.internal.logger.LogDatabaseHelper;

public class DP3TCalibrationHelper {

	public static void setCalibrationTestDeviceName(Context context, String name) {
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(name);
	}

	public static String getCalibrationTestDeviceName(Context context) {
		return AppConfigManager.getInstance(context).getCalibrationTestDeviceName();
	}

	public static void disableCalibrationTestDeviceName(Context context) {
		AppConfigManager.getInstance(context).setCalibrationTestDeviceName(null);
	}

	public static void exportDatabase(Context context, OutputStream targetOut, Runnable successCallback,
			Consumer<Exception> errorCallback) {
		new Thread(() -> {
			ExportDatabaseOpenHelper dbOpenHelper = ExportDatabaseOpenHelper.getInstance(context);
			SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
			LogDatabaseHelper.copyLogDatabase(database);
			DeviceHelper.addDeviceInfoToDatabase(database, context);
			dbOpenHelper.exportDatabaseTo(context, targetOut, successCallback, errorCallback);
		}).start();
	}

}
