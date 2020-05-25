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
import android.content.SharedPreferences;
import android.os.Build;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;

public class ExposureDayStorage {

	private static final int NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS = 10;

	private static final String PREF_KEY_EEXPOSURE_DAYS = "exposureDays";
	private static final String PREF_KEY_LAST_ID = "last_id";

	private static ExposureDayStorage instance;

	private SharedPreferences sp;

	public static synchronized ExposureDayStorage getInstance(Context context) {
		if (instance == null) {
			instance = new ExposureDayStorage(context);
		}
		return instance;
	}

	private ExposureDayStorage(Context context) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
				sp = EncryptedSharedPreferences.create("dp3t_exposuredays_store",
						KEY_ALIAS,
						context,
						EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
						EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
			} else {
				sp = context.getSharedPreferences("dp3t_exposuredays_store_not_encrypted", Context.MODE_PRIVATE);
			}
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	private ExposureDayList getExposureDaysInternal() {
		ExposureDayList list =
				Json.safeFromJson(sp.getString(PREF_KEY_EEXPOSURE_DAYS, "[]"), ExposureDayList.class, ExposureDayList::new);

		DayDate maxAgeForExposureDay = new DayDate().subtractDays(NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS);
		Iterator<ExposureDay> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getExposedDate().isBefore(maxAgeForExposureDay)) {
				iterator.remove();
			}
		}

		Collections.sort(list, (a, b) -> a.getExposedDate().compareTo(b.getExposedDate()));

		return list;
	}

	public ArrayList<ExposureDay> getExposureDays() {
		ExposureDayList list = getExposureDaysInternal();
		Iterator<ExposureDay> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().isDeleted()) {
				iterator.remove();
			}
		}
		return list;
	}

	public void addExposureDay(Context context, ExposureDay exposureDay) {
		ExposureDayList previousExposureDays = getExposureDaysInternal();
		for (ExposureDay previousExposureDay : previousExposureDays) {
			if (previousExposureDay.getExposedDate().equals(exposureDay.getExposedDate())) {
				return;//exposure day was already added
			}
		}

		int id = sp.getInt(PREF_KEY_LAST_ID, 0) + 1;
		exposureDay.setId(id);
		previousExposureDays.add(exposureDay);
		sp.edit()
				.putInt(PREF_KEY_LAST_ID, id)
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(previousExposureDays))
				.apply();

		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public void resetExposureDays() {
		ExposureDayList previousExposureDays = getExposureDaysInternal();
		for (ExposureDay previousExposureDay : previousExposureDays) {
			previousExposureDay.setDeleted(true);
		}
		sp.edit()
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(previousExposureDays))
				.apply();
	}

	public void clear() {
		sp.edit()
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(new ExposureDayList()))
				.apply();
	}

	private static class ExposureDayList extends ArrayList<ExposureDay> {

	}

}
