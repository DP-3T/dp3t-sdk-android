/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;

public class BleClient {

	private static final String TAG = "BleClient";

	private final Context context;
	private BluetoothLeScanner bleScanner;
	private ScanCallback bleScanCallback;
	private GattConnectionThread gattConnectionThread;
	private long minTimeToReconnectToSameDevice = 5 * 60 * 1000;

	public BleClient(Context context) {
		this.context = context;
		gattConnectionThread = new GattConnectionThread();
		gattConnectionThread.start();
	}

	public void setMinTimeToReconnectToSameDevice(long minTimeToReconnectToSameDevice) {
		this.minTimeToReconnectToSameDevice = minTimeToReconnectToSameDevice;
	}

	public void start() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			BroadcastHelper.sendUpdateBroadcast(context);
			return;
		}
		bleScanner = bluetoothAdapter.getBluetoothLeScanner();

		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder()
				.setServiceUuid(new ParcelUuid(BleServer.SERVICE_UUID))
				.build());

		// Scan for Apple devices as iOS does not advertise service uuid when in background,
		// but instead pushes it to the "overflow" area (manufacturer data). For now let's
		// connect to all Apple devices until we find the algorithm used to put the service uuid
		// into the manufacturer data
		scanFilters.add(new ScanFilter.Builder()
				.setManufacturerData(0x004c, new byte[0])
				.build());

		ScanSettings scanSettings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
				.build();

		bleScanCallback = new ScanCallback() {
			public void onScanResult(int callbackType, ScanResult result) {
				if (result.getScanRecord() != null) {
					onDeviceFound(result);
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				Logger.d(TAG, "Batch size " + results.size());
				for (ScanResult result : results) {
					onScanResult(0, result);
				}
			}

			public void onScanFailed(int errorCode) {
				Log.e("ScanCallback", "error: " + errorCode);
			}
		};

		bleScanner.startScan(scanFilters, scanSettings, bleScanCallback);
		Logger.i(TAG, "bleScanner started");
	}

	private Map<String, Long> deviceLastConnected = new HashMap<>();

	public void onDeviceFound(ScanResult scanResult) {
		try {
			BluetoothDevice bluetoothDevice = scanResult.getDevice();
			Log.d(TAG, bluetoothDevice.getAddress() + "; " + scanResult.getScanRecord().getDeviceName());

			if (deviceLastConnected.get(bluetoothDevice.getAddress()) != null &&
					deviceLastConnected.get(bluetoothDevice.getAddress()) > System.currentTimeMillis() -
							minTimeToReconnectToSameDevice) {
				Log.d(TAG, "skipped");
				return;
			}

			int power = scanResult.getScanRecord().getTxPowerLevel();
			if (power == Integer.MIN_VALUE) {
				Logger.d(TAG, "No power levels found for (" + scanResult.getDevice().getName() + "), use default of 12dbm");
				power = 12;
			}

			deviceLastConnected.put(bluetoothDevice.getAddress(), System.currentTimeMillis());

			byte[] payload = scanResult.getScanRecord().getManufacturerSpecificData(BleServer.MANUFACTURER_ID);
			if (payload != null && payload.length == CryptoModule.KEY_LENGTH) {
				// if Android, optimize (meaning: send/read payload directly in the SCAN_RESP)
				Logger.d(TAG, "read star payload from manufatorer data");
				new Database(context)
						.addHandshake(context, payload, power, scanResult.getRssi(),
								System.currentTimeMillis());
			} else {
				gattConnectionThread.addTask(new GattConnectionTask(context, bluetoothDevice, scanResult));
			}
		} catch (Throwable t) {
			t.printStackTrace();
			Logger.e(TAG, t);
		}
	}

	public synchronized void stopScan() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			BroadcastHelper.sendUpdateBroadcast(context);
			return;
		}
		if (bleScanner == null) {
			bleScanner = bluetoothAdapter.getBluetoothLeScanner();
		}
		Logger.i(TAG, "bleScanner stopped");
		Log.d(TAG, "stopping BLE scanner");
		bleScanner.stopScan(bleScanCallback);
		bleScanner = null;
	}

	public synchronized void stop() {
		gattConnectionThread.terminate();
		stopScan();
	}

}
