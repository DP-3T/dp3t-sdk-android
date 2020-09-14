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
import java.security.PublicKey;

import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.util.SignatureUtil;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SignatureVerificationInterceptorTest {

	Context context;
	MockWebServer server;
	BackendBucketRepository bucketRepository;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();

		Logger.init(context, LogLevel.DEBUG);

		ProxyConfig.DISABLE_SYSTEM_PROXY = true;

		server = new MockWebServer();
		PublicKey signaturePublicKey = SignatureUtil.getPublicKeyFromBase64OrThrow(
				"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFaXFSZ2FvYzdMb0p" +
						"jdUx3d3F1OGszNmhVc2dheQp1a0lTR2p2cEtab05vNGZRNWJsekFUV3VBK0E4eklDRnFDOFNXQmlvZkFCRmxqandNeDR2ejlobGVnPT0KL" +
						"S0tLS1FTkQgUFVCTElDIEtFWS0tLS0t");
		bucketRepository = new BackendBucketRepository(context, server.url("/bucket/").toString(), signaturePublicKey);
	}

	@Test
	public void testValidSignature() throws IOException, StatusCodeException {
		String responseString = "{\"forceUpdate\":false,\"forceTraceShutdown\":false,\"infoBox\":null," +
				"\"sdkConfig\":{\"numberOfWindowsForExposure\":3,\"eventThreshold\":0.8,\"badAttenuationThreshold\":73.0," +
				"\"contactAttenuationThreshold\":73.0},\"iOSGaenSdkConfig\":{\"lowerThreshold\":55,\"higherThreshold\":63," +
				"\"factorLow\":1.0,\"factorHigh\":0.5,\"triggerThreshold\":15},\"androidGaenSdkConfig\":{\"lowerThreshold\":55," +
				"\"higherThreshold\":63,\"factorLow\":1.0,\"factorHigh\":0.5,\"triggerThreshold\":15}}";
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody(responseString)
						.addHeader(SignatureUtil.HTTP_HEADER_JWS,
								"eyJhbGciOiJFUzI1NiJ9" +
										".eyJjb250ZW50LWhhc2giOiI1RFhzRTlKRjNjNVpMVWJMUTcxOGhDNDdyWFJyU3l5Z29nSTVzOG8xK2tNPSIsImh" +
										"hc2gtYWxnIjoic2hhLTI1NiIsImlzcyI6ImRwM3QiLCJpYXQiOjE2MDAwODcxNjQsImV4cCI6MTYwMTkwMTU2NH0" +
										".dj-96Xu_XwVwxzPCz5OP4E_hdDWSzUl6sVMvus7_8cEMit-ccshsSAaRSmf_MOgUEL9PCeAt8DtUNFdHW6WnzQ");
			}
		});
		String response = bucketRepository.getGaenExposees(new DayDate(), null).body().string();
		assertEquals(responseString, response);
	}

	@Test
	public void testInvalidSignature() throws IOException, StatusCodeException {
		String responseString = "blaaah";
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody(responseString)
						.addHeader(SignatureUtil.HTTP_HEADER_JWS,
								"eyJhbGciOiJFUzI1NiJ9" +
										".eyJjb250ZW50LWhhc2giOiI1RFhzRTlKRjNjNVpMVWJMUTcxOGhDNDdyWFJyU3l5Z29nSTVzOG8xK2tNPSIsImh" +
										"hc2gtYWxnIjoic2hhLTI1NiIsImlzcyI6ImRwM3QiLCJpYXQiOjE2MDAwODcxNjQsImV4cCI6MTYwMTkwMTU2NH0" +
										".dj-96Xu_XwVwxzPCz5OP4E_hdDWSzUl6sVMvus7_8cEMit-ccshsSAaRSmf_MOgUEL9PCeAt8DtUNFdHW6WnzQ");
			}
		});
		try {
			bucketRepository.getGaenExposees(new DayDate(), null).body().string();
			fail();
		} catch (SignatureException e) {
			assertEquals(e.getMessage(), "Signature mismatch");
		}
	}

	@Test
	public void testInvalidJwt() throws IOException, StatusCodeException {
		String responseString = "blaaah";
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
			assertEquals(e.getMessage(),
					"JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be " +
							"trusted.");
		}
	}

}
