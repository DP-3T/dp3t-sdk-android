/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal;

import android.bluetooth.le.AdvertiseSettings;

public enum BluetoothTxPowerLevel {
	ADVERTISE_TX_POWER_ULTRA_LOW(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW),
	ADVERTISE_TX_POWER_LOW(AdvertiseSettings.ADVERTISE_TX_POWER_LOW),
	ADVERTISE_TX_POWER_MEDIUM(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM),
	ADVERTISE_TX_POWER_HIGH(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

	private final int systemValue;

	BluetoothTxPowerLevel(final int systemValue) {
		this.systemValue = systemValue;
	}

	public int getSystemValue() {
		return systemValue;
	}
}
