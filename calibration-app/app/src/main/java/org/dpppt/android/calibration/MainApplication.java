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
import org.dpppt.android.sdk.util.SignatureUtil;

import okhttp3.CertificatePinner;

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
		PublicKey publicKey = SignatureUtil.getPublicKeyFromBase64OrThrow(
				"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0R" +
						"RZ0FFSndKMkErS2taR0p6QlMzM3dEOUUyaEI1K3VNYgpZcitNU2pOUGhmYzR6Q2w2amdSWkFWVHBKbE" +
						"0wSmI4RERqcDNRUDZhK2VEK1I1SFYyNzhROVN0SUhnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t");
		DP3T.init(context, "org.dpppt.demo", true, publicKey);

		CertificatePinner certificatePinner = new CertificatePinner.Builder()
				.add("demo.dpppt.org", "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=")
				.build();
		DP3T.setCertificatePinner(certificatePinner);
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