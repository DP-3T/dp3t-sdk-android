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

import android.app.AlarmManager;
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
import java.util.Collections;
import java.util.Set;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.R;
import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.storage.ErrorNotificationStorage;
import org.dpppt.android.sdk.internal.storage.models.ActiveNotificationErrors;

public class TracingErrorsBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "TracingErrorsBR";

	private static final String NOTIFICATION_CHANNEL_ID = "dp3t_tracing_service";
	private static final int NOTIFICATION_ID = 1827;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!DP3T.ACTION_UPDATE_ERRORS.equals(intent.getAction()) || !DP3T.isInitialized())
			return;

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		TracingStatus status = DP3T.getStatus(context);
		Collection<ErrorState> errorsForNotification = refreshErrorsForNotification(context, status);
		if (errorsForNotification == null) {
			// do nothing
			Logger.d(TAG, "no notification change");
		} else if (!errorsForNotification.isEmpty()) {
			Logger.d(TAG, "show notification");
			Notification notification = createStatusNotification(context, errorsForNotification);
			notificationManager.notify(NOTIFICATION_ID, notification);
		} else {
			Logger.d(TAG, "dismiss notification");
			notificationManager.cancel(NOTIFICATION_ID);
		}
	}

	private Collection<ErrorState> refreshErrorsForNotification(Context context, TracingStatus status) {
		ErrorNotificationStorage storage = ErrorNotificationStorage.getInstance(context);

		if (!status.isTracingEnabled()) {
			storage.saveActiveErrors(new ActiveNotificationErrors());
			return Collections.emptySet();
		}

		ActiveNotificationErrors savedActiveErrors = storage.getSavedActiveErrors();

		long now = System.currentTimeMillis();
		long newSuppressedUntil = now + SyncErrorState.getInstance().getErrorNotificationGracePeriod();
		// sync errors have already been delayed, so don't suppress them for the notification
		Collection<ErrorState> unsuppressableErrors = ErrorHelper.getDelayableSyncErrors();
		long nextChange = savedActiveErrors.refreshActiveErrors(status.getErrors(), now, newSuppressedUntil, unsuppressableErrors);
		if (nextChange > now) {
			refreshNotificationDelayed(context, nextChange);
			Logger.d(TAG, "scheduled notification invalidation in " + (nextChange - now) / 1000 + "s");
		}

		storage.saveActiveErrors(savedActiveErrors);
		Set<ErrorState> unsuppressedErrors = savedActiveErrors.getUnsuppressedErrors(now);

		Set<ErrorState> lastShownErrors = storage.getLastShownErrors();
		if (unsuppressedErrors.equals(lastShownErrors)) {
			return null;
		}

		storage.saveLastShownErrors(unsuppressedErrors);

		return unsuppressedErrors;
	}

	private Notification createStatusNotification(Context context, Collection<ErrorState> notificationErrors) {
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
				.setOnlyAlertOnce(true)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(errorText))
				.setPriority(NotificationCompat.PRIORITY_MAX);

		return builder.build();
	}

	private String getNotificationErrorText(Context context, Collection<ErrorState> errors) {
		StringBuilder sb = new StringBuilder(context.getString(R.string.dp3t_sdk_service_notification_errors));
		for (ErrorState error : errors) {
			sb.append("\n").append(error.getErrorString(context));
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

	private static void refreshNotificationDelayed(Context context, long triggerAtMillis) {
		Intent broadcast = new Intent(context, TracingErrorsBroadcastReceiver.class)
				.setAction(DP3T.ACTION_UPDATE_ERRORS);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcast, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
	}

}
