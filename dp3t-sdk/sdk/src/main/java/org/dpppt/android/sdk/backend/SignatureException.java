package org.dpppt.android.sdk.backend;

public class SignatureException extends io.jsonwebtoken.security.SignatureException {

	public SignatureException(String message) {
		super(message);
	}

	public SignatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
