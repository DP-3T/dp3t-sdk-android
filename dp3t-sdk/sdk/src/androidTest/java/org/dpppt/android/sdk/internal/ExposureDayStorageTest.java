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

import java.util.Arrays;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.internal.storage.ExposureDayStorage;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ExposureDayStorageTest {

	private Context context;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();
		DP3T.init(context, new ApplicationInfo("", ""), null);
	}

	@Test
	public void testInsertion() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis())));
		assertEquals(1, eds.getExposureDays().size());
	}

	@Test
	public void testMultiInsertion() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10)));
		assertEquals(1, eds.getExposureDays().size());
		assertEquals(new DayDate(), eds.getExposureDays().get(0).getExposedDate());

		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis()),
				new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis() - 20)));
		assertEquals(2, eds.getExposureDays().size());
		assertEquals(new DayDate().subtractDays(1), eds.getExposureDays().get(0).getExposedDate());
		assertEquals(new DayDate(), eds.getExposureDays().get(1).getExposedDate());

		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis())));
		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis())));
		assertEquals(2, eds.getExposureDays().size());
		assertEquals(new DayDate().subtractDays(1), eds.getExposureDays().get(0).getExposedDate());
		assertEquals(new DayDate(), eds.getExposureDays().get(1).getExposedDate());
	}

	@Test
	public void testReset() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis())));
		eds.resetExposureDays();
		assertEquals(0, eds.getExposureDays().size());
	}

	@Test
	public void testResetReadd() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10)));
		eds.resetExposureDays();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis())));
		assertEquals(0, eds.getExposureDays().size());
	}

	@Test
	public void testResetAddNewDay() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10)));
		eds.resetExposureDays();
		eds.addExposureDays(context, Arrays.asList(new ExposureDay(-1, new DayDate().subtractDays(1),
				System.currentTimeMillis())));
		assertEquals(1, eds.getExposureDays().size());
	}

	@Test
	public void testKeepTestsFor14DaysAfterReport() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		//should be returned in getExposureDays()
		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-1, new DayDate().subtractDays(11), System.currentTimeMillis() - 10)));
		//should not be considered because the report date is more than 14 days in the past
		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-2, new DayDate().subtractDays(16),
						System.currentTimeMillis() - 15 * 24 * 60 * 60 * 1000)));
		assertEquals(1, eds.getExposureDays().size());
	}


	@Test
	public void testKeepTestsFor10DaysAfterReport() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		//change config to keep exposure days only for 10 days after the report date
		DP3T.setNumberOfDaysToKeepExposedDays(context, 10);
		//should be returned in getExposureDays()
		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-1, new DayDate().subtractDays(11), System.currentTimeMillis() - 10)));
		//should not be considered because the report date is more than 10 days in the past
		eds.addExposureDays(context,
				Arrays.asList(new ExposureDay(-2, new DayDate().subtractDays(16),
						System.currentTimeMillis() - 11 * 24 * 60 * 60 * 1000)));
		assertEquals(1, eds.getExposureDays().size());
	}

}
