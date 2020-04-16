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

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.R;
import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.internal.gatt.BleClient;
import org.dpppt.android.sdk.internal.gatt.BleServer;
import org.dpppt.android.sdk.internal.logger.Logger;

public class TracingService extends Service {

	private static final String TAG = "TracingService";

	public static final String ACTION_START = TracingService.class.getCanonicalName() + ".ACTION_START";
	public static final String ACTION_STOP = TracingService.class.getCanonicalName() + ".ACTION_STOP";

	public static final String EXTRA_ADVERTISE = TracingService.class.getCanonicalName() + ".EXTRA_ADVERTISE";
	public static final String EXTRA_RECEIVE = TracingService.class.getCanonicalName() + ".EXTRA_RECEIVE";
	public static final String EXTRA_SCAN_INTERVAL = TracingService.class.getCanonicalName() + ".EXTRA_SCAN_INTERVAL";
	public static final String EXTRA_SCAN_DURATION = TracingService.class.getCanonicalName() + ".EXTRA_SCAN_DURATION";

	private static String NOTIFICATION_CHANNEL_ID = "dp3t_tracing_service";
	private static int NOTIFICATION_ID = 1827;
	private Handler handler;

	private PowerManager.WakeLock wl;

	private BleServer bleServer;
	private BleClient bleClient;

	private boolean startAdvertising;
	private boolean startReceiveing;
	private long scanInterval;
	private long scanDuration;

	public TracingService() { }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return START_STICKY;
		}

		if (wl == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getPackageName() + ":TracingServiceWakeLock");
			wl.acquire();
		}

		Logger.i(TAG, "service started");
		Log.d(TAG, "onHandleIntent() with " + intent.getAction());

		scanInterval = intent.getLongExtra(EXTRA_SCAN_INTERVAL, 5 * 60 * 1000);
		scanDuration = intent.getLongExtra(EXTRA_SCAN_DURATION, 30 * 1000);

		startAdvertising = intent.getBooleanExtra(EXTRA_ADVERTISE, true);
		startReceiveing = intent.getBooleanExtra(EXTRA_RECEIVE, true);

		if (ACTION_START.equals(intent.getAction())) {
			startForeground(NOTIFICATION_ID, createForegroundNotification());
			start();
		} else if (ACTION_STOP.equals(intent.getAction())) {
			stopForegroundService();
		}

		return START_STICKY;
	}

	private Notification createForegroundNotification() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel();
		}

		Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		PendingIntent contentIntent = null;
		if (launchIntent != null) {
			contentIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		TracingStatus status = DP3T.getStatus(this);
		if (status.getErrors().size() > 0) {
			String errorText = getNotificationErrorText(status.getErrors());
			return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
					.setOngoing(true)
					.setContentTitle(getString(R.string.dp3t_sdk_service_notification_title))
					.setSmallIcon(R.drawable.ic_handshakes)
					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(errorText))
					.setPriority(NotificationCompat.PRIORITY_DEFAULT)
					.setContentIntent(contentIntent)
					.build();
		} else {
			return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
					.setOngoing(true)
					.setContentTitle(getString(R.string.dp3t_sdk_service_notification_title))
					.setContentText(getString(R.string.dp3t_sdk_service_notification_text))
					.setSmallIcon(R.drawable.ic_handshakes)
					.setPriority(NotificationCompat.PRIORITY_LOW)
					.setContentIntent(contentIntent)
					.build();
		}
	}

	private String getNotificationErrorText(ArrayList<TracingStatus.ErrorState> errors) {
		StringBuilder b = new StringBuilder(getString(R.string.dp3t_sdk_service_notification_errors)).append("\n");
		for (int i = 0; i < errors.size(); i++) {
			TracingStatus.ErrorState error = errors.get(i);
			b.append(getString(error.getErrorString()));
			if (i < errors.size() - 1) {
				b.append(", ");
			}
		}
		return b.toString();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createNotificationChannel() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelName = getString(R.string.dp3t_sdk_service_notification_channel);
		NotificationChannel channel =
				new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
	}

	private void start() {
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		handler = new Handler();

		startTracing();
	}

	private void startTracing() {
		Log.d(TAG, "startTracing()");

		try {
			Notification notification = createForegroundNotification();

			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(NOTIFICATION_ID, notification);

			startClient();
			startServer();
		} catch (Throwable t) {
			t.printStackTrace();
			Logger.e(TAG, t);
		}

		handler.postDelayed(() -> {
			stopScanning();
			scheduleNextRun(this, scanInterval);
		}, scanDuration);
	}

	private void stopScanning() {
		if (bleClient != null) {
			bleClient.stopScan();
		}
	}

	public static void scheduleNextRun(Context context, long scanInterval) {
		long now = System.currentTimeMillis();
		long delay = scanInterval - (now % scanInterval);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, TracingServiceBroadcastReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delay, pendingIntent);
	}

	private void stopForegroundService() {
		stopClient();
		stopServer();
		stopForeground(true);
		wl.release();
		stopSelf();
	}

	@Override
	public void onDestroy() {
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		Log.d(TAG, "onDestroy()");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void startServer() {
		stopServer();
		if (startAdvertising) {
			bleServer = new BleServer(this);
			bleServer.start();
			bleServer.startAdvertising();
		}
	}

	private void stopServer() {
		if (bleServer != null) {
			bleServer.stop();
			bleServer = null;
		}
	}

	private void startClient() {
		stopClient();
		if (startReceiveing) {
			bleClient = new BleClient(this);
			bleClient.setMinTimeToReconnectToSameDevice(scanInterval);
			bleClient.start();
		}
	}

	private void stopClient() {
		if (bleClient != null) {
			bleClient.stop();
			bleClient = null;
		}
	}

}
