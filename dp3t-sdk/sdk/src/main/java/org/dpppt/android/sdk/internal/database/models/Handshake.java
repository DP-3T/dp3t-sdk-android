/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database.models;

public class Handshake {

	private int id;
	private long timestamp;
	private byte[] star;
	private int txPowerLevel;
	private int rssi;

	public Handshake(int id, long timstamp, byte[] star, int txPowerLevel, int rssi) {
		this.id = id;
		this.timestamp = timstamp;
		this.star = star;
		this.txPowerLevel = txPowerLevel;
		this.rssi = rssi;
	}

	public byte[] getEphId() {
		return star;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getTxPowerLevel() {
		return txPowerLevel;
	}

	public int getRssi() {
		return rssi;
	}

}
