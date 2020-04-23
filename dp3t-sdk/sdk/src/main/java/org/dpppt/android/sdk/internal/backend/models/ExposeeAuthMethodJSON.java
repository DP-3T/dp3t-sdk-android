/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend.models;

public class ExposeeAuthMethodJSON implements ExposeeAuthMethod {

	private String value;

	public ExposeeAuthMethodJSON(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
