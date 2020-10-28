/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertTrue;
import static org.dpppt.android.sdk.internal.nearby.ExposureWindowMatchingWorker.convertAttenuationDurationsToMinutes;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class ExposureNotifactionThresholdTest {

	private Context context;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();
	}

	@Test
	public void testDefault() {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.clearPreferences();
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 15, 0, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 10, 10, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 0, 30, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 30, 30, 30 }));

		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 14, 1, 0 }));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 5, 19, 0 }));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 0, 29, 30 }));
	}


	@Test
	public void testChangedConfiguration() {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.clearPreferences();
		appConfigManager.setAttenuationFactorLow(0.5f);
		appConfigManager.setAttenuationFactorMedium(2f);
		appConfigManager.setMinDurationForExposure(10);

		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 20, 0, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 10, 3, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 0, 30, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 30, 0, 0 }));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 30, 30, 30 }));

		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 1, 4, 0 }));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 15, 1, 0 }));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, new int[] { 19, 0, 30 }));
	}

	@Test
	public void testDefaultWithSeconds() {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		appConfigManager.clearPreferences();
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 14*60+1, 0, 0 })));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 10*60, 10*60, 0 })));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 0, 29*60+1, 0 })));
		assertTrue(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 30*60, 30*60, 30*60 })));

		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 14*60, 1, 0 })));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 5*60, 19*60, 0 })));
		assertFalse(ExposureWindowMatchingWorker.isExposureLimitReached(context, convertAttenuationDurationsToMinutes(new int[] { 0, 29*60, 30*60 })));
	}

}
