/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
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
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.util.SignatureUtil;

import okhttp3.CertificatePinner;

public class MainApplication extends Application {

	public static final String BASE_URL = "https://demo.dpppt.org/";

	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(sdkReceiver, DP3T.getUpdateIntentFilter());
		Logger.init(getApplicationContext(), LogLevel.DEBUG);
		initDP3T(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationUtil.createNotificationChannel(this);
		}
	}

	public static void initDP3T(Context context) {
		PublicKey signaturePublicKey = SignatureUtil.getPublicKeyFromBase64OrThrow(
				"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0R" +
						"RZ0FFdkxXZHVFWThqcnA4aWNSNEpVSlJaU0JkOFh2UgphR2FLeUg2VlFnTXV2Zk1JcmxrNk92QmtKeH" +
						"dhbUdNRnFWYW9zOW11di9rWGhZdjF1a1p1R2RjREJBPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t");
		DP3T.init(context,
				new ApplicationInfo("org.dpppt.demo", BASE_URL, BASE_URL),
				signaturePublicKey);

		if (!BuildConfig.DEBUG) {
			CertificatePinner certificatePinner = new CertificatePinner.Builder()
					.add("demo.dpppt.org", "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=")
					.build();
			DP3T.setCertificatePinner(certificatePinner);
		}

		String userAgent = BuildConfig.APPLICATION_ID + ";" +
				BuildConfig.VERSION_NAME + ";" +
				BuildConfig.VERSION_CODE + ";" +
				"Android;" +
				Build.VERSION.SDK_INT;
		DP3T.setUserAgent(userAgent);
	}

	@Override
	public void onTerminate() {
		unregisterReceiver(sdkReceiver);
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