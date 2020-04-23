/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import java.util.UUID;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.logger.Logger;

public class BleServer {

	private static final String TAG = "BleServer";

	public static final UUID SERVICE_UUID = UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB");
	public static final UUID TOTP_CHARACTERISTIC_UUID = UUID.fromString("8c8494e3-bab5-1848-40a0-1b06991c0001");

	private final Context context;
	private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartFailure(int errorCode) {
			Logger.e(TAG, "advertise onStartFailure: " + errorCode);
		}

		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			Logger.i(TAG, "advertise onStartSuccess: " + settingsInEffect.toString());
		}
	};
	private BluetoothAdapter mAdapter;
	private BluetoothLeAdvertiser mLeAdvertiser;

	public BleServer(Context context) {
		this.context = context;
	}

	private byte[] getAdvertiseData() {
		CryptoModule cryptoModule = CryptoModule.getInstance(context);
		byte[] advertiseData = cryptoModule.getCurrentEphId().getData();
		String calibrationTestDeviceName = AppConfigManager.getInstance(context).getCalibrationTestDeviceName();
		if (calibrationTestDeviceName != null) {
			byte[] nameBytes = calibrationTestDeviceName.getBytes();
			for (int i = 0; i < AppConfigManager.CALIBRATION_TEST_DEVICE_NAME_LENGTH; i++) {
				advertiseData[i] = nameBytes[i];
			}
			long curMinInEpoch = ((System.currentTimeMillis() - cryptoModule.getCurrentEpochStart()) / (60 * 1000));
			byte[] minData = Long.toString(curMinInEpoch).getBytes();
			advertiseData[AppConfigManager.CALIBRATION_TEST_DEVICE_NAME_LENGTH] = minData[0];
			if (minData.length > 1) {
				advertiseData[AppConfigManager.CALIBRATION_TEST_DEVICE_NAME_LENGTH + 1] = minData[1];
			}
		}
		return advertiseData;
	}

	public void startAdvertising() {
		BluetoothManager mManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

		if (mManager == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			throw new UnsupportedOperationException("whea's mah bluetooth?");

		mAdapter = mManager.getAdapter();
		mLeAdvertiser = mAdapter.getBluetoothLeAdvertiser();

		AdvertiseData.Builder advBuilder = new AdvertiseData.Builder();
		advBuilder.setIncludeTxPowerLevel(true);
		advBuilder.addServiceUuid(new ParcelUuid(SERVICE_UUID));
		advBuilder.addServiceData(new ParcelUuid(SERVICE_UUID), getAdvertiseData());
		advBuilder.setIncludeDeviceName(false);

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);

		AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
		settingBuilder.setAdvertiseMode(appConfigManager.getBluetoothAdvertiseMode().getValue());
		settingBuilder.setTxPowerLevel(appConfigManager.getBluetoothTxPowerLevel().getValue());
		settingBuilder.setConnectable(true);

		mLeAdvertiser.startAdvertising(settingBuilder.build(), advBuilder.build(), advertiseCallback);
	}

	public void stopAdvertising() {
		if (mLeAdvertiser != null) {
			mLeAdvertiser.stopAdvertising(advertiseCallback);
		}
	}

	public void stop() {
		stopAdvertising();
		mAdapter = null;
	}

}
