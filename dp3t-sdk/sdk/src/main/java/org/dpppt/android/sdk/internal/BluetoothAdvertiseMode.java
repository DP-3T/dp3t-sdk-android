/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal;

import android.bluetooth.le.AdvertiseSettings;

public enum BluetoothAdvertiseMode {
	ADVERTISE_MODE_LOW_POWER(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER),
	ADVERTISE_MODE_BALANCED(AdvertiseSettings.ADVERTISE_MODE_BALANCED),
	ADVERTISE_MODE_LOW_LATENCY(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);

	private final int systemValue;

	BluetoothAdvertiseMode(final int systemValue) {
		this.systemValue = systemValue;
	}

	public int getSystemValue() {
		return systemValue;
	}
}
