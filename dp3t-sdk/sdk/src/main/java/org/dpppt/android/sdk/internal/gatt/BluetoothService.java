package org.dpppt.android.sdk.internal.gatt;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.TracingController;
import org.dpppt.android.sdk.internal.TracingService;
import org.dpppt.android.sdk.internal.logger.Logger;

public class BluetoothService implements TracingController {

	private static final String TAG = "BluetoothService";

	public static final String EXTRA_ADVERTISE = TracingService.class.getCanonicalName() + ".EXTRA_ADVERTISE";
	public static final String EXTRA_RECEIVE = TracingService.class.getCanonicalName() + ".EXTRA_RECEIVE";
	public static final String EXTRA_SCAN_INTERVAL = TracingService.class.getCanonicalName() + ".EXTRA_SCAN_INTERVAL";
	public static final String EXTRA_SCAN_DURATION = TracingService.class.getCanonicalName() + ".EXTRA_SCAN_DURATION";

	private final Context context;

	private final Handler scanningHandler = new Handler();

	private BleServer bleServer;
	private BleClient bleClient;

	private boolean doAdvertising;
	private boolean doReceiving;

	private long scanInterval;
	private long scanDuration;

	public BluetoothService(Context context) {
		this.context = context;
	}

	@Override
	public void setParams(Bundle extras) {
		scanInterval = extras.getLong(EXTRA_SCAN_INTERVAL, AppConfigManager.DEFAULT_SCAN_INTERVAL);
		scanDuration = extras.getLong(EXTRA_SCAN_DURATION, AppConfigManager.DEFAULT_SCAN_DURATION);
		doAdvertising = extras.getBoolean(EXTRA_ADVERTISE, true);
		doReceiving = extras.getBoolean(EXTRA_RECEIVE, true);
	}

	@Override
	public void start() {
		scanningHandler.removeCallbacksAndMessages(null);

		restartClient();
		restartServer();
	}

	@Override
	public void restartClient() {
		scanningHandler.removeCallbacksAndMessages(null);

		//also restart server here to generate a new mac-address so we get rediscovered by apple devices
		startServer();

		BluetoothState bluetoothState = startClient();
		if (bluetoothState == BluetoothState.NOT_SUPPORTED) {
			Logger.e(TAG, "bluetooth not supported");
			return;
		}

		scanningHandler.postDelayed(() -> {
			stopScanning();
			TracingService.scheduleNextClientRestart(context, scanInterval);
		}, scanDuration);
	}

	@Override
	public void restartServer() {
		BluetoothState bluetoothState = startServer();
		if (bluetoothState == BluetoothState.NOT_SUPPORTED) {
			Logger.e(TAG, "bluetooth not supported");
			return;
		}

		TracingService.scheduleNextServerRestart(context);
	}

	private BluetoothState startServer() {
		stopServer();
		if (doAdvertising) {
			bleServer = new BleServer(context);

			Logger.d(TAG, "startAdvertising");
			BluetoothState advertiserState = bleServer.startAdvertising();
			return advertiserState;
		}
		return null;
	}

	private void stopServer() {
		if (bleServer != null) {
			bleServer.stop();
			bleServer = null;
		}
	}

	private BluetoothState startClient() {
		stopClient();
		if (doReceiving) {
			bleClient = new BleClient(context);
			BluetoothState clientState = bleClient.start();
			return clientState;
		}
		return null;
	}

	private void stopScanning() {
		if (bleClient != null) {
			bleClient.stopScan();
		}
	}

	private void stopClient() {
		if (bleClient != null) {
			bleClient.stop();
			bleClient = null;
		}
	}

	@Override
	public void stop() {
		stopClient();
		stopServer();
	}

	@Override
	public void destroy() {
		scanningHandler.removeCallbacksAndMessages(null);
	}

}
