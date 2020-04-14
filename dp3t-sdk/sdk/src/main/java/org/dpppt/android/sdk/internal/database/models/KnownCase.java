/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database.models;

import static org.dpppt.android.sdk.internal.util.Base64Util.fromBase64;

public class KnownCase {

	private int id;
	private String day;
	private String key;

	public KnownCase(int id, String day, String key) {
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

	public String getKey() {
		return key;
	}

	public byte[] getParsedKey() {
		return fromBase64(key);
	}

}
