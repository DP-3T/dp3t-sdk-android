/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.*;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.logger.Logger;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class GattConnectionTask {

	private static final String TAG = "BleClient";

	private static final long GATT_READ_TIMEOUT = 10 * 1000l;

	Context context;
	BluetoothDevice bluetoothDevice;
	ScanResult scanResult;

	private BluetoothGatt bluetoothGatt;
	private long startTime;

	public GattConnectionTask(Context context, BluetoothDevice bluetoothDevice, ScanResult scanResult) {
		this.context = context;
		this.bluetoothDevice = bluetoothDevice;
		this.scanResult = scanResult;
	}

	public void execute() {
		Log.d(TAG, "connecting GATT...");
		Logger.i(TAG, "Trying to connect to: " + bluetoothDevice.getAddress());

		final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
				super.onConnectionStateChange(gatt, status, newState);
				if (newState == BluetoothProfile.STATE_CONNECTING) {
					Log.d("BluetoothGattCallback", "connecting... " + status);
				} else if (newState == BluetoothProfile.STATE_CONNECTED) {
					Log.d("BluetoothGattCallback", "connected " + status);
					Log.d("BluetoothGattCallback", "requesting mtu...");
					Logger.i("BluetoothGattCallback", "Gatt Connection established");
					gatt.requestMtu(512);
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED || newState == BluetoothProfile.STATE_DISCONNECTING) {
					Log.d("BluetoothGattCallback", "disconnected " + status);
					Logger.i("BluetoothGattCallback", "Gatt Connection disconnected " + status);
					finish();
				}
			}

			@Override
			public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
				Log.d("BluetoothGattCallback", "discovering services...");
				gatt.discoverServices();
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				BluetoothGattService service = gatt.getService(BleServer.SERVICE_UUID);

				if (service == null) {
					Log.e("BluetoothGattCallback", "No GATT service for " + BleServer.SERVICE_UUID + " found, status=" + status);
					Logger.i("BluetoothGattCallback", "Could not find our GATT service");
					finish();
					return;
				}

				Log.d("BluetoothGattCallback", "Service " + service.getUuid() + " found");

				BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServer.TOTP_CHARACTERISTIC_UUID);

				boolean initiatedRead = gatt.readCharacteristic(characteristic);
				if (!initiatedRead) {
					Log.e("BluetoothGattCallback", "Failed to initiate characteristic read");
					Logger.e("BluetoothGattCallback", "Failed to read");
				} else {
					Logger.d("BluetoothGattCallback", "Read initiated");
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				Log.d("onCharacteristicRead", "[status:" + status + "] " + characteristic.getUuid() + ": " +
						Arrays.toString(characteristic.getValue()));

				if (characteristic.getUuid().equals(BleServer.TOTP_CHARACTERISTIC_UUID)) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						addHandshakeToDatabase(characteristic.getValue(), gatt.getDevice().getAddress(),
								scanResult.getScanRecord().getTxPowerLevel(), scanResult.getRssi());
					} else {
						Log.e("BluetoothGattCallback", "Failed to read characteristic. Status: " + status);

						// TODO error
					}
				}
				finish();
				Logger.d("BluetoothGattCallback", "Closed Gatt Connection");
			}
		};

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
		} else {
			bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback);
		}

		startTime = System.currentTimeMillis();
	}

	public void addHandshakeToDatabase(byte[] starValue, String macAddress, int rxPowerLevel, int rssi) {
		try {
			String base64String = toBase64(starValue);
			Log.d("received", base64String);
			new Database(context)
					.addHandshake(context, starValue, rxPowerLevel, rssi, System.currentTimeMillis());
			Logger.d(TAG, "received " + base64String);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void checkForTimeout() {
		if (System.currentTimeMillis() - startTime > GATT_READ_TIMEOUT) {
			finish();
		}
	}

	public boolean isFinished() {
		return bluetoothGatt == null;
	}

	public void finish() {
		if (bluetoothGatt != null) {
			Log.d(TAG, "closeGattAndConnectToNextDevice (disconnect() and then close())");
			Logger.d(TAG, "Calling disconnect() and close(): " + bluetoothGatt.getDevice().getAddress());
			// Order matters! Call disconnect() before close() as the latter de-registers our client
			// and essentially makes disconnect a NOP.
			bluetoothGatt.disconnect();
			bluetoothGatt.close();
			bluetoothGatt = null;
		}
		Logger.i(TAG, "Reset and wait for next BLE device");
	}

}
