/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.calibration;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.security.PublicKey;

import org.dpppt.android.calibration.util.NotificationUtil;
import org.dpppt.android.calibration.util.PreferencesUtil;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.ProcessUtil;

public class MainApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		if (ProcessUtil.isMainProcess(this)) {
			registerReceiver(sdkReceiver, DP3T.getUpdateIntentFilter());
			initDP3T(this);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationUtil.createNotificationChannel(this);
		}
		Logger.init(getApplicationContext(), LogLevel.DEBUG);
	}

	public static void initDP3T(Context context) {
		PublicKey publicKey = null;
		DP3T.init(context, "org.dpppt.demo", true, publicKey);
	}

	@Override
	public void onTerminate() {
		if (ProcessUtil.isMainProcess(this)) {
			unregisterReceiver(sdkReceiver);
		}
		super.onTerminate();
	}

	private BroadcastReceiver sdkReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (DP3T.getStatus(context).getExposureDays().size() > 0 && !PreferencesUtil.isExposedNotificationShown(context)) {
				NotificationUtil.showNotification(context, R.string.push_exposed_title,
						R.string.push_exposed_text, R.drawable.ic_handshakes);
				PreferencesUtil.setExposedNotificationShown(context);
			}
		}
	};

}