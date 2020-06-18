/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.util.Base64Util;
import org.dpppt.android.sdk.util.SignatureUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class JwtsTest {

	@Test(expected = SignatureException.class)
	public void verifyExpired() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String jws = "eyJhbGciOiJFUzI1NiJ9.eyJjb250ZW50LWhhc2giOiJsTzd3TDBkOFl5MFBSaU" +
				"w5NGhUa2txMkRXNUxXVjlPNi9zRWNZVDJHZ2t3PSIsImhhc2gtYWxnIjoic2hhLTI1Ni" +
				"IsImlzcyI6ImRwM3QiLCJpYXQiOjE1ODgwODk2MDAsImV4cCI6MTU4OTkwNDAwMCwiYm" +
				"F0Y2gtcmVsZWFzZS10aW1lIjoiMTU4ODA4OTYwMDAwMCJ9.1uiVGBOWqD8jLKm0_EOmN" +
				"MMgHr4FQOsD1ci4iWR1QMitg_MPgtMiLY7i9nT0hM29IxLD75bls5M65YmSjODpgQ";
		String pubkey64 = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0" +
				"FRWUlLb1pJemowREFRY0RRZ0FFdXZQelFqN0w0MkxldXJhRGIrSEtPTnAvbm1mcQppbG" +
				"g2YXRnZ1BBeklJcEttTmlQbG4vNWFYK0VZM1VEQldVK1hpN09QbTAxakUxWUE1bHpYY3" +
				"U1N1hnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t";
		byte[] expectedHash = Base64Util.fromBase64("lO7wL0d8Yy0PRiL94hTkkq2DW5LWV9O6/sEcYT2Ggkw=");
		PublicKey publicKey = SignatureUtil.getPublicKeyFromBase64(pubkey64);
		byte[] contentHash = SignatureUtil.getVerifiedContentHash(jws, publicKey);
		Assert.assertArrayEquals(expectedHash, contentHash);
	}

	@Test(expected = SignatureException.class)
	public void verifyInvalidSignature() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String jws = "eyJhbGciOiJFUzI1NiJ9.eyJjb250ZW50LWhhc2giOiJsTzd3TDBkOFl5MFBSaU" +
				"w5NGhUa2txMkRXNUxXVjlPNi9zRWNZVDJHZ2t3PSIsImhhc2gtYWxnIjoic2hhLTI1Ni" +
				"IsImlzcyI6ImRwM3QiLCJpYXQiOjE1ODgwODk2MDAsImV4cCI6MTU4OTkwNDAwMCwiYm" +
				"F0Y2gtcmVsZWFzZS10aW1lIjoiMTU4ODA4OTYwMDAwMCJ9.1uiVGBOWqD8jLKm0_EOmN" +
				"MMgHr4FQOsD1ci4iWR1QMitg_MPgtbuggedbuggedbuggedbuggedbuggedbugged";
		String pubkey64 = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0" +
				"FRWUlLb1pJemowREFRY0RRZ0FFdXZQelFqN0w0MkxldXJhRGIrSEtPTnAvbm1mcQppbG" +
				"g2YXRnZ1BBeklJcEttTmlQbG4vNWFYK0VZM1VEQldVK1hpN09QbTAxakUxWUE1bHpYY3" +
				"U1N1hnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t";
		PublicKey publicKey = SignatureUtil.getPublicKeyFromBase64(pubkey64);
		byte[] contentHash = SignatureUtil.getVerifiedContentHash(jws, publicKey);
		Assert.fail("should have thrown a SignatureException");
	}

}
