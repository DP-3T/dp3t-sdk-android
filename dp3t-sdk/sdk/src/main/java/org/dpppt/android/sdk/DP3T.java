/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.backend.models.ApplicationInfo;
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethod;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.ErrorHelper;
import org.dpppt.android.sdk.internal.SyncWorker;
import org.dpppt.android.sdk.internal.TracingService;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.database.models.MatchedContact;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.dpppt.android.sdk.internal.util.ProcessUtil;

public class DP3T {

	private static final String TAG = "DP3T Interface";

	public static final String UPDATE_INTENT_ACTION = "org.dpppt.android.sdk.UPDATE_ACTION";

	private static String appId;

	public static void init(Context context, String appId) {
		init(context, appId, false);
	}

	public static void init(Context context, String appId, boolean enableDevDiscoveryMode) {
		if (ProcessUtil.isMainProcess(context)) {
			DP3T.appId = appId;
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			appConfigManager.setAppId(appId);
			appConfigManager.setDevDiscoveryModeEnabled(enableDevDiscoveryMode);
			appConfigManager.triggerLoad();

			executeInit(context);
		}
	}

	public static void init(Context context, ApplicationInfo applicationInfo) {
		if (ProcessUtil.isMainProcess(context)) {
			DP3T.appId = applicationInfo.getAppId();
			AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
			appConfigManager.setManualApplicationInfo(applicationInfo);

			executeInit(context);
		}
	}

	private static void executeInit(Context context) {
		CryptoModule.getInstance(context).init();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		boolean advertising = appConfigManager.isAdvertisingEnabled();
		boolean receiving = appConfigManager.isReceivingEnabled();
		if (advertising || receiving) {
			start(context, advertising, receiving);
		}
	}

	private static void checkInit() throws IllegalStateException {
		if (appId == null) {
			throw new IllegalStateException("You have to call DP3T.init() in your application onCreate()");
		}
	}

	public static void start(Context context) {
		start(context, true, true);
	}

	protected static void start(Context context, boolean advertise, boolean receive) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(advertise);
		appConfigManager.setReceivingEnabled(receive);
		long scanInterval = appConfigManager.getScanInterval();
		long scanDuration = appConfigManager.getScanDuration();
		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_START);
		intent.putExtra(TracingService.EXTRA_ADVERTISE, advertise);
		intent.putExtra(TracingService.EXTRA_RECEIVE, receive);
		intent.putExtra(TracingService.EXTRA_SCAN_INTERVAL, scanInterval);
		intent.putExtra(TracingService.EXTRA_SCAN_DURATION, scanDuration);
		ContextCompat.startForegroundService(context, intent);
		SyncWorker.startSyncWorker(context);
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static boolean isStarted(Context context) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		return appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled();
	}

	public static void sync(Context context) {
		checkInit();
		try {
			SyncWorker.doSync(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SQLiteException ignored) {
			// has been handled upstream
		}
	}

	public static TracingStatus getStatus(Context context) {
		checkInit();
		Database database = new Database(context);
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		Collection<TracingStatus.ErrorState> errorStates = ErrorHelper.checkTracingErrorStatus(context);
		List<MatchedContact> matchedContacts = database.getMatchedContacts();
		InfectionStatus infectionStatus;
		if (appConfigManager.getIAmInfected()) {
			infectionStatus = InfectionStatus.INFECTED;
		} else if (matchedContacts.size() > 0) {
			infectionStatus = InfectionStatus.EXPOSED;
		} else {
			infectionStatus = InfectionStatus.HEALTHY;
		}
		return new TracingStatus(
				database.getContacts().size(),
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				appConfigManager.getLastSyncDate(),
				infectionStatus,
				matchedContacts,
				errorStates
		);
	}

	public static void sendIAmInfected(Context context, Date onset, ExposeeAuthMethod exposeeAuthMethod,
			ResponseCallback<Void> callback) {
		checkInit();

		DayDate onsetDate = new DayDate(onset.getTime());
		ExposeeRequest exposeeRequest = CryptoModule.getInstance(context).getSecretKeyForPublishing(onsetDate, exposeeAuthMethod);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		try {
			appConfigManager.getBackendReportRepository(context).addExposee(exposeeRequest, exposeeAuthMethod,
					new ResponseCallback<Void>() {
						@Override
						public void onSuccess(Void response) {
							appConfigManager.setIAmInfected(true);
							CryptoModule.getInstance(context).reset();
							callback.onSuccess(response);
						}

						@Override
						public void onError(Throwable throwable) {
							callback.onError(throwable);
						}
					});
		} catch (IllegalStateException e) {
			callback.onError(e);
			Logger.e(TAG, e);
		}
	}

	public static void stop(Context context) {
		checkInit();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.setAdvertisingEnabled(false);
		appConfigManager.setReceivingEnabled(false);

		Intent intent = new Intent(context, TracingService.class).setAction(TracingService.ACTION_STOP);
		context.startService(intent);
		SyncWorker.stopSyncWorker(context);
		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(DP3T.UPDATE_INTENT_ACTION);
	}

	public static void clearData(Context context, Runnable onDeleteListener) {
		checkInit();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		if (appConfigManager.isAdvertisingEnabled() || appConfigManager.isReceivingEnabled()) {
			throw new IllegalStateException("Tracking must be stopped for clearing the local data");
		}

		CryptoModule.getInstance(context).reset();
		appConfigManager.clearPreferences();
		Logger.clear();
		Database db = new Database(context);
		db.recreateTables(response -> onDeleteListener.run());
	}

}
