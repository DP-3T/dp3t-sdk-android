/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database.models;

import org.dpppt.android.sdk.internal.crypto.EphId;

public class Handshake {

	private int id;
	private long timestamp;
	private EphId ephId;
	private int txPowerLevel;
	private int rssi;

	public Handshake(int id, long timstamp, EphId ephId, int txPowerLevel, int rssi) {
		this.id = id;
		this.timestamp = timstamp;
		this.ephId = ephId;
		this.txPowerLevel = txPowerLevel;
		this.rssi = rssi;
	}

	public EphId getEphId() {
		return ephId;
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
