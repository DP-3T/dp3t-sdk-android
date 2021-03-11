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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.InfectionStatus;
import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.backend.ProxyConfig;
import org.dpppt.android.sdk.internal.backend.models.GaenKey;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.nearby.TestGoogleExposureClient;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.ExposeeAuthMethodJson;
import org.dpppt.android.sdk.util.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TEKReleaseTest {

	Context context;
	MockWebServer server;
	TestGoogleExposureClient testGoogleExposureClient;
	Instrumentation.ActivityMonitor monitor;
	Instrumentation mInstrumentation;

	@Before
	public void setup() throws IOException {
		mInstrumentation = InstrumentationRegistry.getInstrumentation();
		context = mInstrumentation.getTargetContext();

		Logger.init(context, LogLevel.DEBUG);

		ProxyConfig.DISABLE_SYSTEM_PROXY = true;
		GaenStateHelper.SET_GAEN_AVAILABILITY_AVAILABLE_FOR_TESTS = true;

		server = new MockWebServer();
		server.start();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		DP3T.init(context, new ApplicationInfo(server.url("/bucket/").toString(), server.url("/report/").toString()),
				null);
		appConfigManager.setTracingEnabled(false);
		DP3T.clearData(context);
		DP3T.init(context, new ApplicationInfo(server.url("/bucket/").toString(), server.url("/report/").toString()),
				null);
		appConfigManager.setTracingEnabled(true);
	}

	@Test
	public void testIAmInfected() throws Exception {
		testGoogleExposureClient = new TestGoogleExposureClient(context);
		GoogleExposureClient.wrapTestClient(testGoogleExposureClient);
		testGoogleExposureClient.setTime(System.currentTimeMillis());

		Activity activity = startEmptyActivity();

		AtomicInteger exposedFakeRequestCounter = new AtomicInteger(0);
		AtomicInteger exposedRequestCounter = new AtomicInteger(0);
		ArrayList<Integer> sentRollingStartNumbers = new ArrayList<>();

		//Onset Date is 4 days ago
		long onsetDate = System.currentTimeMillis() - 1000 * 60 * 60 * 96;
		int expectedNumberOfTEKToday = 5;

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				String body = new String(request.getBody().readByteArray());
				if (request.getPath().equals("/bucket/v2/gaen/exposed")) {
					GaenRequest gaenRequest = Json.fromJson(body, GaenRequest.class);
					int fake = gaenRequest.isFake();
					if (fake == 1) {
						exposedFakeRequestCounter.getAndIncrement();
					} else {
						for (GaenKey k : gaenRequest.getGaenKeys()) {
							if (!k.isFake()) {
								sentRollingStartNumbers.add(k.getRollingStartNumber());
							}
						}
						exposedRequestCounter.getAndIncrement();
					}
				}
				return new MockResponse().setResponseCode(200);
			}
		});

		CountDownLatch countDownLatch = new CountDownLatch(1);
		DP3T.sendIAmInfected(activity, new Date(onsetDate), new ExposeeAuthMethodJson(""), new ResponseCallback<Void>() {
			@Override
			public void onSuccess(Void response) {
				countDownLatch.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				countDownLatch.countDown();
			}
		});
		countDownLatch.await();

		AtomicLong today = new AtomicLong(System.currentTimeMillis());
		try {
			new SyncWorker.SyncImpl(context, today.get()).doSyncBlocking();
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertEquals(1, exposedRequestCounter.get());
		assertEquals(0, exposedFakeRequestCounter.get());
		assertFalse(DP3T.isTracingEnabled(context));
		assertEquals(InfectionStatus.INFECTED, DP3T.getStatus(context).getInfectionStatus());
		assertEquals(expectedNumberOfTEKToday, sentRollingStartNumbers.size());
		for (int k : sentRollingStartNumbers) {
			assertTrue(k >= DateUtil.getRollingStartNumberForDate(onsetDate));
			assertTrue(k <= DateUtil.getRollingStartNumberForDate(today.get()));
		}

		AtomicLong tomorrow = new AtomicLong(today.get() + 1000 * 60 * 60 * 24);
		testGoogleExposureClient.setTime(tomorrow.get());

		try {
			new SyncWorker.SyncImpl(context, tomorrow.get()).doSyncBlocking();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mInstrumentation.removeMonitor(monitor);

		assertEquals(1, exposedRequestCounter.get());
		assertEquals(0, exposedFakeRequestCounter.get());
		assertFalse(DP3T.isTracingEnabled(context));
		assertEquals(InfectionStatus.INFECTED, DP3T.getStatus(context).getInfectionStatus());
	}


	private Activity startEmptyActivity() {
		Instrumentation.ActivityMonitor monitor = mInstrumentation.addMonitor(TestActivity.class.getName(), null, false);
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName(mInstrumentation.getTargetContext(), TestActivity.class.getName());
		mInstrumentation.startActivitySync(intent);

		Activity activity = mInstrumentation.waitForMonitor(monitor);
		assertNotNull(activity);
		return activity;
	}

}
