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
import java.util.concurrent.atomic.AtomicLong;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.nearby.TestGoogleExposureClient;
import org.dpppt.android.sdk.models.ApplicationInfo;
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
		AppConfigManager.getInstance(context).clearPreferences();

		Logger.init(context, LogLevel.DEBUG);

		testGoogleExposureClient = new TestGoogleExposureClient();
		GoogleExposureClient.wrapTestClient(testGoogleExposureClient);

		server = new MockWebServer();
		server.start();
		DP3T.init(context, new ApplicationInfo("test", server.url("/bucket/").toString(), server.url("/report/").toString()),
				null);
	}

	@Test
	public void testSyncStartAtMorning() throws Exception {
		AtomicLong time = new AtomicLong(yesterdayAt8am());

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
				return new MockResponse()
						.setResponseCode(200)
						.setBody("randomdatabecauseitdoesnotmatter")
						.addHeader("x-published-until", time.get() - 2 * 60 * 60 * 1000l);
			}
		});

		for (int i = 0; i < 21 + 24; i++) {
			new SyncWorker.SyncImpl(context, time.get()).doSync();
			time.set(time.get() + 1 * 60 * 60 * 1000l);
		}

		assertEquals(40, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testSyncStartEvening() throws Exception {
		AtomicLong time = new AtomicLong(yesterdayAt8pm());

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
				return new MockResponse()
						.setResponseCode(200)
						.setBody("randomdatabecauseitdoesnotmatter")
						.addHeader("x-published-until", time.get() - 2 * 60 * 60 * 1000l);
			}
		});

		for (int i = 0; i < 24; i++) {
			new SyncWorker.SyncImpl(context, time.get()).doSync();
			time.set(time.get() + 1 * 60 * 60 * 1000l);
		}

		assertEquals(30, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	@Test
	public void testSync204() throws Exception {
		AtomicLong time = new AtomicLong(yesterdayAt8am());
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
				return new MockResponse().setResponseCode(204).addHeader("x-published-until", time.get());
			}
		});

		new SyncWorker.SyncImpl(context, time.get()).doSync();
		assertEquals(0, testGoogleExposureClient.getProvideDiagnosisKeysCounter());
	}

	private long yesterdayAt8am() {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTimeInMillis();
	}

	private long yesterdayAt8pm() {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR_OF_DAY, 20);
		cal.set(Calendar.MINUTE, 0);
		return cal.getTimeInMillis();
	}

}
