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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import androidx.core.util.Consumer;

import java.io.OutputStream;

import org.dpppt.android.sdk.internal.export.DeviceHelper;
import org.dpppt.android.sdk.internal.export.ExportDatabaseOpenHelper;
import org.dpppt.android.sdk.internal.logger.LogDatabaseHelper;

public class DP3TCalibrationHelper {

	private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";
	private static final String PREF_EXPERIMENT_NAME = "experimentName";

	private static DP3TCalibrationHelper instance;
	private final SharedPreferences sharedPrefs;

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

	public static synchronized DP3TCalibrationHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DP3TCalibrationHelper(context);
		}
		return instance;
	}

	private DP3TCalibrationHelper(Context context) {
		sharedPrefs = context.getSharedPreferences("DP3TCalibrationHelper", Context.MODE_PRIVATE);
	}

	public void setCalibrationTestDeviceName(String name) {
		sharedPrefs.edit().putString(PREF_CALIBRATION_TEST_DEVICE_NAME, name).apply();
	}

	public String getCalibrationTestDeviceName() {
		BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
		return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, myDevice.getName().replace(' ', '_'));
	}

	public void setExperimentName(String name) {
		sharedPrefs.edit().putString(PREF_EXPERIMENT_NAME, name).apply();
	}

	public String getExperimentName() {
		return sharedPrefs.getString(PREF_EXPERIMENT_NAME, "");
	}

}
