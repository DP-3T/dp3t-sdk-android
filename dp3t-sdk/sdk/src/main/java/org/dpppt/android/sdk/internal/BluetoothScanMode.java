/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal;

import android.bluetooth.le.ScanSettings;

public enum BluetoothScanMode {
	SCAN_MODE_LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER),
	SCAN_MODE_BALANCED(ScanSettings.SCAN_MODE_BALANCED),
	SCAN_MODE_LOW_LATENCY(ScanSettings.SCAN_MODE_LOW_LATENCY),
	SCAN_MODE_OPPORTUNISTIC(ScanSettings.SCAN_MODE_OPPORTUNISTIC);

	private final int systemValue;

	BluetoothScanMode(final int systemValue) {
		this.systemValue = systemValue;
	}

	public int getSystemValue() {
		return systemValue;
	}
}
