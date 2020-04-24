/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database.models;

public class KnownCase {

	private int id;
	private String day;
	private byte[] key;

	public KnownCase(int id, String day, byte[] key) {
		this.id = id;
		this.day = day;
		this.key = key;
	}

	public int getId() {
		return id;
	}

	public String getDay() {
		return day;
	}

	public byte[] getKey() {
		return key;
	}

}
