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

import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExposureDayStorageTest {

	private Context context;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();
	}

	@Test
	public void testInsertion() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis()));
		assert (eds.getExposureDays().size() == 1);
	}

	@Test
	public void testMultiInsertion() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10));
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis()));
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis() - 20));
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis()));
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis()));
		assert (eds.getExposureDays().size() == 2);
	}

	@Test
	public void testReset() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis()));
		eds.resetExposureDays();
		assert (eds.getExposureDays().size() == 0);
	}

	@Test
	public void testResetReadd() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10));
		eds.resetExposureDays();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis()));
		assert (eds.getExposureDays().size() == 0);
	}

	@Test
	public void testResetAddNewDay() {
		ExposureDayStorage eds = ExposureDayStorage.getInstance(context);
		eds.clear();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate(), System.currentTimeMillis() - 10));
		eds.resetExposureDays();
		eds.addExposureDay(context, new ExposureDay(-1, new DayDate().subtractDays(1), System.currentTimeMillis()));
		assert (eds.getExposureDays().size() == 1);
	}

}
