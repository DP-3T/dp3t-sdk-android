package org.dpppt.android.sdk.internal.backend.models;

public class ExposeeAuthMethodAuthorization implements ExposeeAuthMethod {

	private String authorization;

	public ExposeeAuthMethodAuthorization(String authorization) {
		this.authorization = authorization;
	}

	public String getAuthorization() {
		return authorization;
	}

}
