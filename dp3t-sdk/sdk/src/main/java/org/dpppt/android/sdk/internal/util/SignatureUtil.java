package org.dpppt.android.sdk.internal.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;

public class SignatureUtil {

	public static final String HTTP_HEADER_JWS = "signature";
	public static final String HASH_ALGO = "SHA-256";

	private static final String JWS_CLAIM_CONTENT_HASH = "content-hash";

	public static PublicKey parsePublicKeyHeader(String publicKeyHeader) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String pubkey = new String(Base64Util.fromBase64(publicKeyHeader));
		byte[] pubkeyRaw = Base64Util.fromBase64(pubkey.replaceAll("-+(BEGIN|END) PUBLIC KEY-+", "").trim());
		return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pubkeyRaw));
	}

	public static PublicKey parsePublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] pubkeyRaw = Base64Util.fromBase64(publicKey);
		return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pubkeyRaw));
	}

	public static byte[] getVerifiedContentHash(String jws, PublicKey publicKey) throws SignatureException {
		Jws<Claims> claimsJws = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(jws);
		String hash64 = claimsJws.getBody().get(JWS_CLAIM_CONTENT_HASH, String.class);
		return Base64Util.fromBase64(hash64);
	}

}
