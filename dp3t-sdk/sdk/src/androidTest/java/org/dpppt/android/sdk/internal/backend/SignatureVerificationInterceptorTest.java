/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.util.Base64Util;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.util.SignatureUtil;
import org.junit.Before;
import org.junit.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.dpppt.android.sdk.util.SignatureUtil.JWS_CLAIM_CONTENT_HASH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SignatureVerificationInterceptorTest {

	Context context;
	MockWebServer server;
	BackendBucketRepository bucketRepository;
	KeyPair keyPair;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();

		Logger.init(context, LogLevel.DEBUG);

		ProxyConfig.DISABLE_SYSTEM_PROXY = true;

		server = new MockWebServer();
		keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);

		bucketRepository = new BackendBucketRepository(context, server.url("/bucket/").toString(), keyPair.getPublic());
	}

	private String getJwtForContent(String content) {
		HashMap<String, Object> claims = new HashMap<>();
		try {
			MessageDigest digest = MessageDigest.getInstance(SignatureUtil.HASH_ALGO);
			claims.put(JWS_CLAIM_CONTENT_HASH, Base64Util.toBase64(digest.digest(content.getBytes())));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return Jwts.builder().addClaims(claims).signWith(keyPair.getPrivate()).compact();
	}

	@Test
	public void testValidSignature() throws IOException, StatusCodeException {
		String responseString = "someRandomContent";
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {

				return new MockResponse()
						.setResponseCode(200)
						.setBody(responseString)
						.addHeader(SignatureUtil.HTTP_HEADER_JWS, getJwtForContent(responseString));
			}
		});
		String response = bucketRepository.getGaenExposees(new DayDate(), null).body().string();
		assertEquals(responseString, response);
	}

	@Test
	public void testInvalidSignature() throws IOException, StatusCodeException {
		String responseString = "someRandomContent";
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody(responseString)
						.addHeader(SignatureUtil.HTTP_HEADER_JWS, getJwtForContent("differentContent"));
			}
		});
		try {
			bucketRepository.getGaenExposees(new DayDate(), null).body().string();
			fail();
		} catch (SignatureException e) {
			assertEquals("Signature mismatch", e.getMessage());
		}
	}

	@Test
	public void testInvalidJwt() throws IOException, StatusCodeException {
		String responseString = "someRandomContent";
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody(responseString)
						.addHeader(SignatureUtil.HTTP_HEADER_JWS,
								"eyJhbGciOiJFUzI1NiJ9.eyJjb250ZW50LWhhc2giOiJsTzd3TDBkOFl5MFBSaU" +
										"w5NGhUa2txMkRXNUxXVjlPNi9zRWNZVDJHZ2t3PSIsImhhc2gtYWxnIjoic2hhLTI1Ni" +
										"IsImlzcyI6ImRwM3QiLCJpYXQiOjE1ODgwODk2MDAsImV4cCI6MTU4OTkwNDAwMCwiYm" +
										"F0Y2gtcmVsZWFzZS10aW1lIjoiMTU4ODA4OTYwMDAwMCJ9.1uiVGBOWqD8jLKm0_EOmN" +
										"MMgHr4FQOsD1ci4iWR1QMitg_MPgtbuggedbuggedbuggedbuggedbuggedbugged");
			}
		});
		try {
			bucketRepository.getGaenExposees(new DayDate(), null).body().string();
			fail();
		} catch (SignatureException e) {
			assertEquals("JWT signature does not match locally computed signature. " +
					"JWT validity cannot be asserted and should not be trusted.", e.getMessage());
		}
	}

}
