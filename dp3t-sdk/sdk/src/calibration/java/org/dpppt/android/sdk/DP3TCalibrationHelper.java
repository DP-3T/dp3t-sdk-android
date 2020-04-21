package org.dpppt.android.sdk;

import android.content.Context;

import java.io.OutputStream;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.database.Database;

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

	public static void exportDb(Context context, OutputStream targetOut, Runnable onExportedListener) {
		Database db = new Database(context);
		db.exportTo(context, targetOut, response -> onExportedListener.run());
	}

	public static void start(Context context, boolean advertise, boolean receive) {
		DP3T.start(context, advertise, receive);
	}

}
