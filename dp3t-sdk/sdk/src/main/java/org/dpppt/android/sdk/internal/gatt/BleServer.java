/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;

public class BleServer {

	public static final UUID SERVICE_UUID = UUID.fromString("8c8494e3-bab5-1848-40a0-1b06991c0000");
	public static final int MANUFACTURER_ID = 0xabba;
	public static final UUID TOTP_CHARACTERISTIC_UUID = UUID.fromString("8c8494e3-bab5-1848-40a0-1b06991c0001");

	private final Context context;
	private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartFailure(int errorCode) {
			Log.d("advertise", "onStartFailure: " + errorCode);
		}

		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			Log.d("advertise", "onStartSuccess: " + settingsInEffect.toString());
		}
	};
	private BluetoothAdapter mAdapter;
	private BluetoothGattServer mGattServer;
	private BluetoothLeAdvertiser mLeAdvertiser;

	public BleServer(Context context) {
		this.context = context;
	}

	public void start() {
		BluetoothManager mManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

		if (mManager == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			throw new UnsupportedOperationException("whea's mah bluetooth?");

		mAdapter = mManager.getAdapter();
		if (mAdapter.isEnabled()) {
			mGattServer = mManager.openGattServer(context, createGattServerCallback());

			setupService();
		}
	}

	private BluetoothGattServerCallback createGattServerCallback() {
		return new BluetoothGattServerCallback() {
			@Override
			public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
				Log.d("GattServer", "Our gatt server connection state changed, new state " + Integer.toString(newState));
			}

			@Override
			public void onServiceAdded(int status, BluetoothGattService service) {
				Log.d("GattServer", "Our gatt server service was added.");
			}

			@Override
			public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
					BluetoothGattCharacteristic characteristic) {
				Log.d("GattServer", "Our gatt characteristic was read.");
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
						characteristic.getValue());
			}

			@Override
			public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
					BluetoothGattCharacteristic characteristic, boolean preparedWrite,
					boolean responseNeeded, int offset, byte[] value) {
				Log.d("GattServer", "We have received a write request for one of our hosted characteristics");
			}

			@Override
			public void onNotificationSent(BluetoothDevice device, int status) {
				Log.d("GattServer", "onNotificationSent");
			}

			@Override
			public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
					BluetoothGattDescriptor descriptor) {
				Log.d("GattServer", "Gatt server descriptor was read.");
			}

			@Override
			public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
					boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
				Log.d("GattServer", "Gatt server descriptor was written.");
			}

			@Override
			public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
				Log.d("GattServer", "Gatt server on execute write.");
			}
		};
	}

	private byte[] getAdvertiseData() {
		byte[] advertiseData = CryptoModule.getInstance(context).getCurrentEphId();
		String calibrationTestDeviceName = AppConfigManager.getInstance(context).getCalibrationTestDeviceName();
		if (calibrationTestDeviceName != null) {
			byte[] nameBytes = calibrationTestDeviceName.getBytes();
			for (int i = 0; i < AppConfigManager.CALIBRATION_TEST_DEVICE_NAME_LENGTH; i++) {
				advertiseData[i] = nameBytes[i];
			}
		}
		return advertiseData;
	}

	private void setupService() {
		BluetoothGattService oldService = mGattServer.getService(SERVICE_UUID);
		if (oldService != null) {
			mGattServer.removeService(oldService);
		}

		BluetoothGattCharacteristic testCharacteristic = new BluetoothGattCharacteristic(
				TOTP_CHARACTERISTIC_UUID,
				BluetoothGattCharacteristic.PROPERTY_READ,
				BluetoothGattCharacteristic.PERMISSION_READ
		);

		BluetoothGattService gattService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

		testCharacteristic.setValue(getAdvertiseData());

		gattService.addCharacteristic(testCharacteristic);
		mGattServer.addService(gattService);
	}

	public void startAdvertising() {
		mLeAdvertiser = mAdapter.getBluetoothLeAdvertiser();

		AdvertiseData.Builder advBuilder = new AdvertiseData.Builder().setIncludeTxPowerLevel(true);
		advBuilder.addServiceUuid(new ParcelUuid(SERVICE_UUID));
		advBuilder.setIncludeDeviceName(false);

		AdvertiseData scanResponse = new AdvertiseData.Builder()
				.setIncludeDeviceName(false).setIncludeTxPowerLevel(false)
				.addManufacturerData(MANUFACTURER_ID, getAdvertiseData())
				.build();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);

		AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
		settingBuilder.setAdvertiseMode(appConfigManager.getBluetoothAdvertiseMode().getValue());
		settingBuilder.setTxPowerLevel(appConfigManager.getBluetoothTxPowerLevel().getValue());
		settingBuilder.setConnectable(true);

		mLeAdvertiser.startAdvertising(settingBuilder.build(), advBuilder.build(), scanResponse, advertiseCallback);
	}

	public void stopAdvertising() {
		if (mLeAdvertiser != null) {
			mLeAdvertiser.stopAdvertising(advertiseCallback);
		}
	}

	public void stop() {
		stopAdvertising();
		if (mGattServer != null) {
			mGattServer.close();
		}
		mGattServer = null;
		mAdapter = null;
	}

}
