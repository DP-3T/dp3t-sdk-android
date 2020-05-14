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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.R;
import org.dpppt.android.sdk.TracingStatus;

public class TracingErrorsBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "TracingErrorsBR";

	private static final String NOTIFICATION_CHANNEL_ID = "dp3t_tracing_service";
	private static final int NOTIFICATION_ID = 1827;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!DP3T.ACTION_UPDATE_ERRORS.equals(intent.getAction()))
			return;

		TracingStatus status = DP3T.getStatus(context);

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = createStatusNotification(context, status);
		if (notification != null) {
			notificationManager.notify(NOTIFICATION_ID, notification);
		} else {
			notificationManager.cancel(NOTIFICATION_ID);
		}
	}

	private Notification createStatusNotification(Context context, TracingStatus status) {
		Set<TracingStatus.ErrorState> notificationErrors = new HashSet<>(status.getErrors());

		// TODO: in case google really wants to handle bluetooth and location errors itself, uncomment the following lines
		//notificationErrors.remove(TracingStatus.ErrorState.LOCATION_SERVICE_DISABLED);
		//notificationErrors.remove(TracingStatus.ErrorState.BLE_DISABLED);

		if (!status.isTracingEnabled() || notificationErrors.isEmpty()) {
			return null;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(context);
		}

		Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		PendingIntent contentIntent = null;
		if (launchIntent != null) {
			contentIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		String errorText = getNotificationErrorText(context, notificationErrors);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_handshakes)
				.setContentIntent(contentIntent)
				.setContentTitle(context.getString(R.string.dp3t_sdk_service_notification_title))
				.setContentText(errorText)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(errorText))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT);

		return builder.build();
	}

	private String getNotificationErrorText(Context context, Collection<TracingStatus.ErrorState> errors) {
		StringBuilder sb = new StringBuilder(context.getString(R.string.dp3t_sdk_service_notification_errors)).append("\n");
		String sep = "";
		for (TracingStatus.ErrorState error : errors) {
			sb.append(sep).append(context.getString(error.getErrorString()));
			sep = ", ";
		}
		return sb.toString();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createNotificationChannel(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String channelName = context.getString(R.string.dp3t_sdk_service_notification_channel);
		NotificationChannel channel =
				new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
	}

}
