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

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.InfectionStatus;
import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.internal.backend.ProxyConfig;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GaenStateHelper;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.nearby.TestGoogleExposureClient;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class SyncWorkerTest {

	Context context;
	MockWebServer server;
	TestGoogleExposureClient testGoogleExposureClient;

	@Before
	public void setup() throws IOException {
		context = InstrumentationRegistry.getInstrumentation().getContext();

		Logger.init(context, LogLevel.DEBUG);

		testGoogleExposureClient = new TestGoogleExposureClient(context);
		GoogleExposureClient.wrapTestClient(testGoogleExposureClient);
		ProxyConfig.DISABLE_SYSTEM_PROXY = true;
		GaenStateHelper.SET_GAEN_AVAILABILITY_AVAILABLE_FOR_TESTS = true;

		server = new MockWebServer();
		server.start();
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		DP3T.init(context, new ApplicationInfo("test", server.url("/bucket/").toString(), server.url("/report/").toString()),
				null);
		appConfigManager.setTracingEnabled(false);
		DP3T.clearData(context);
		DP3T.init(context, new ApplicationInfo("test", server.url("/bucket/").toString(), server.url("/report/").toString()),
				null);
		appConfigManager.setTracingEnabled(true);
	}

	@Test
	public void testSyncStartAtMorning() {
		if (!BuildConfig.FLAVOR.equals("production")) {
			throw new IllegalStateException("Wrong Build Variant. Make sure to run this Test with the production build variant.");
		}
		AtomicLong time = new AtomicLong(yesterdayAt3am());

		server.setDispatcher(new Dispatcher() {
			int requestCounter = 0;

			@Override
			public MockResponse dispatch(RecordedRequest request) {
				requestCounter++;
				if (requestCounter % 7 == 0) {
					//fail every 7th request
					return new MockResponse()
							.setResponseCode(503);
				} else {
					return new MockResponse()
							.setResponseCode(200)
							.setBody("randomdatabecauseitdoesnotmatter")
							.addHeader("x-published-until", time.get() - 2 * 60 * 60 * 1000l);
				}
			}
		});

		for (int i = 0; i < 21 + 24; i++) {
			try {
				new SyncWorker.SyncImpl(context, time.get()).doSync();
			} catch (Exception e) {
				e.printStackTrace();
			}
			time.set(time.get() + 1 * 60 * 60 * 1000l);
		}

		assertEquals(40, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testSyncStartEvening() throws Exception {
		if (!BuildConfig.FLAVOR.equals("production")) {
			throw new IllegalStateException("Wrong Build Variant. Make sure to run this Test with the production build variant.");
		}
		AtomicLong time = new AtomicLong(yesterdayAt8pm());

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody("randomdatabecauseitdoesnotmatter")
						.addHeader("x-published-until", time.get() - 2 * 60 * 60 * 1000l);
			}
		});

		for (int i = 0; i < 480; i++) {
			new SyncWorker.SyncImpl(context, time.get()).doSync();
			time.set(time.get() + 1 * 60 * 60 * 1000l);
		}

		assertEquals(410, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testSyncDelayedInEvening() throws Exception {
		if (!BuildConfig.FLAVOR.equals("production")) {
			throw new IllegalStateException("Wrong Build Variant. Make sure to run this Test with the production build variant.");
		}
		AtomicLong time = new AtomicLong(yesterdayAt8am());

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse()
						.setResponseCode(200)
						.setBody("randomdatabecauseitdoesnotmatter")
						.addHeader("x-published-until", time.get() - 2 * 60 * 60 * 1000l);
			}
		});

		//8am
		new SyncWorker.SyncImpl(context, time.get()).doSync();

		//3am +1
		time.set(time.get() + 19 * 60 * 60 * 1000l);
		new SyncWorker.SyncImpl(context, time.get()).doSync();

		//7am +1
		time.set(time.get() + 4 * 60 * 60 * 1000l);
		new SyncWorker.SyncImpl(context, time.get()).doSync();

		//7pm +1
		time.set(time.get() + 12 * 60 * 60 * 1000l);
		new SyncWorker.SyncImpl(context, time.get()).doSync();

		assertEquals(30, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testSync204() throws Exception {
		AtomicLong time = new AtomicLong(yesterdayAt8am());
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse().setResponseCode(204).addHeader("x-published-until", time.get());
			}
		});

		new SyncWorker.SyncImpl(context, time.get()).doSync();
		assertEquals(0, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testExposure() {
		TestGoogleExposureClient.ExposureTestParameters params = new TestGoogleExposureClient.ExposureTestParameters();
		params.attenuationDurations = new int[] { 20, 0, 0 };
		params.daysSinceLastExposure = 1;
		params.matchedKeyCount = 1;

		testExposure(params);

		TracingStatus status = DP3T.getStatus(context);
		assertEquals(InfectionStatus.EXPOSED, status.getInfectionStatus());
	}

	@Test
	public void testExposureNotLongEnough() {
		TestGoogleExposureClient.ExposureTestParameters params = new TestGoogleExposureClient.ExposureTestParameters();
		params.attenuationDurations = new int[] { 10, 8, 0 };
		params.daysSinceLastExposure = 1;
		params.matchedKeyCount = 1;

		testExposure(params);

		TracingStatus status = DP3T.getStatus(context);
		assertEquals(InfectionStatus.HEALTHY, status.getInfectionStatus());
	}

	private void testExposure(TestGoogleExposureClient.ExposureTestParameters params) {
		AtomicLong time = new AtomicLong(yesterdayAt8am());
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				String content;
				if (request.getPath().endsWith(String.valueOf(new DayDate(time.get()).subtractDays(1).getStartOfDayTimestamp()))) {

					content = Json.toJson(params);
				} else {
					content = "randomcontent";
				}
				return new MockResponse()
						.setResponseCode(200)
						.setBody(content)
						.addHeader("x-published-until", time.get());
			}
		});

		try {
			new SyncWorker.SyncImpl(context, time.get()).doSync();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long yesterdayAt3am() {
		Calendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR_OF_DAY, 3);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTimeInMillis();
	}

	private long yesterdayAt8am() {
		Calendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTimeInMillis();
	}

	private long yesterdayAt8pm() {
		Calendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR_OF_DAY, 20);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTimeInMillis();
	}

}
