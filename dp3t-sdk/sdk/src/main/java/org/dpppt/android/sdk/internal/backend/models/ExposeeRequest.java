/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend.models;

import org.dpppt.android.sdk.backend.models.ExposeeAuthMethodJson;

public class ExposeeRequest {

	private String key;
	private long keyDate;
	private ExposeeAuthMethodJson authData;

	public ExposeeRequest(String key, long keyDate, ExposeeAuthMethodJson authData) {
		this.key = key;
		this.keyDate = keyDate;
		this.authData = authData;
	}

	public String getKey() {
		return key;
	}

	public long getKeyDate() {
		return keyDate;
	}

	public ExposeeAuthMethodJson getAuthData() {
		return authData;
	}

}
