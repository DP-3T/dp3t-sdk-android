/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.backend.models;

public class ExposeeAuthMethodJson implements ExposeeAuthMethod {

	private String value;

	public ExposeeAuthMethodJson(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
