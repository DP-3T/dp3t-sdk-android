package org.dpppt.android.sdk.internal.backend;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.dpppt.android.sdk.internal.util.SignatureUtil;

import io.jsonwebtoken.security.SignatureException;
import okhttp3.Interceptor;
import okhttp3.Response;

public class SignatureVerificationInterceptor implements Interceptor {

	private static final long PEEK_MEMORY_LIMIT = 64 * 1024 * 1024L;

	private final PublicKey publicKey;

	public SignatureVerificationInterceptor(@NonNull PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());

		if (!response.isSuccessful()) {
			return response;
		}

		String jwsHeader = response.headers().get(SignatureUtil.HTTP_HEADER_JWS);
		if (jwsHeader == null) {
			throw new SignatureException("JWS header not found");
		}

		// <debugging pk>
		// TODO: remove this; use field `publicKey` instead
		PublicKey publicKey = this.publicKey;
		if (publicKey == null && response.headers().get("x-public-key") != null) {
			try {
				publicKey = SignatureUtil.parsePublicKeyHeader(response.headers().get("x-public-key"));
			} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				throw new SignatureException("Invalid public key header", e);
			}
		}
		// </debugging pk>

		if (publicKey == null) {
			throw new SignatureException("Public key not specified");
		}

		byte[] signedContentHash = SignatureUtil.getVerifiedContentHash(jwsHeader, publicKey);

		byte[] body = response.peekBody(PEEK_MEMORY_LIMIT).bytes();

		byte[] actualContentHash;
		try {
			MessageDigest digest = MessageDigest.getInstance(SignatureUtil.HASH_ALGO);
			actualContentHash = digest.digest(body);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		if (!Arrays.equals(actualContentHash, signedContentHash)) {
			throw new SignatureException("Signature mismatch");
		}

		return response;
	}

}
