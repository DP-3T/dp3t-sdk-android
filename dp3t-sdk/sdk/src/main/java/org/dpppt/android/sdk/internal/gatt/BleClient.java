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
import android.content.ContentValues;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;

import static org.dpppt.android.sdk.internal.gatt.BleServer.SERVICE_UUID;

public class BleClient {

	private static final String TAG = "BleClient";

	private final Context context;
	private BluetoothLeScanner bleScanner;
	private ScanCallback bleScanCallback;
	private GattConnectionThread gattConnectionThread;

	public BleClient(Context context) {
		this.context = context;
		gattConnectionThread = new GattConnectionThread();
		gattConnectionThread.start();
	}

	public BluetoothState start() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			return bluetoothAdapter == null ? BluetoothState.NOT_SUPPORTED : BluetoothState.DISABLED;
		}
		bleScanner = bluetoothAdapter.getBluetoothLeScanner();
		if (bleScanner == null) {
			return BluetoothState.NOT_SUPPORTED;
		}

		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder()
				.setServiceUuid(new ParcelUuid(SERVICE_UUID))
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

		BluetoothServiceStatus bluetoothServiceStatus = BluetoothServiceStatus.getInstance(context);

		bleScanCallback = new ScanCallback() {
			private static final String TAG = "ScanCallback";

			public void onScanResult(int callbackType, ScanResult result) {
				bluetoothServiceStatus.updateScanStatus(BluetoothServiceStatus.SCAN_OK);
				if (result.getScanRecord() != null) {
					onDeviceFound(result);
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				bluetoothServiceStatus.updateScanStatus(BluetoothServiceStatus.SCAN_OK);
				Logger.d(TAG, "Batch size " + results.size());
				for (ScanResult result : results) {
					onScanResult(0, result);
				}
			}

			public void onScanFailed(int errorCode) {
				bluetoothServiceStatus.updateScanStatus(errorCode);
				Logger.e(TAG, "error: " + errorCode);
			}
		};

		Logger.i(TAG, "starting BLE scanner");
		bleScanner.startScan(scanFilters, scanSettings, bleScanCallback);

		return BluetoothState.ENABLED;
	}

	public void onDeviceFound(ScanResult scanResult) {
		try {
			BluetoothDevice bluetoothDevice = scanResult.getDevice();
			Logger.d(TAG, "found " + bluetoothDevice.getAddress() + "; " + scanResult.getScanRecord().getDeviceName());

			int power = scanResult.getScanRecord().getTxPowerLevel();
			if (power == Integer.MIN_VALUE) {
				Logger.d(TAG, "No power levels found for (" + scanResult.getDevice().getName() + "), use default of 12dbm");
				power = 12;
			}

			byte[] payload = scanResult.getScanRecord().getServiceData(new ParcelUuid(SERVICE_UUID));
			if (payload != null && payload.length == CryptoModule.KEY_LENGTH) {
				// if Android, optimize (meaning: send/read payload directly in the SCAN_RESP)
				Logger.d(TAG, "read ephid payload from servicedata data");
				ContentValues handshakeData = new Database(context)
						.addHandshake(context, payload, power, scanResult.getRssi(), System.currentTimeMillis());
				Logger.i(TAG, "saved handshake: " + handshakeData.toString());
			} else {
				gattConnectionThread.addTask(new GattConnectionTask(context, bluetoothDevice, scanResult));
			}
		} catch (Throwable t) {
			Logger.e(TAG, t);
		}
	}

	public synchronized void stopScan() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			bleScanner = null;
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			return;
		}
		if (bleScanner != null) {
			Logger.i(TAG, "stopping BLE scanner");
			bleScanner.stopScan(bleScanCallback);
			bleScanner = null;
		}
	}

	public synchronized void stop() {
		gattConnectionThread.terminate();
		stopScan();
	}

}
