/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.DayDate;
import org.dpppt.android.sdk.models.ExposureDay;

public class ExposureDayStorage {

	private static final Type EXPOSUREDAY_LIST_TYPE = new TypeToken<LinkedList<ExposureDay>>() { }.getType();

	private static final int NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS = 14;

	private static final String PREF_KEY_EEXPOSURE_DAYS = "exposureDays";
	private static final String PREF_KEY_LAST_ID = "last_id";

	private static ExposureDayStorage instance;

	private SharedPreferences esp;

	public static synchronized ExposureDayStorage getInstance(Context context) {
		if (instance == null) {
			instance = new ExposureDayStorage(context);
		}
		return instance;
	}

	private ExposureDayStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			esp = EncryptedSharedPreferences.create("dp3t_exposuredays_store",
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	private List<ExposureDay> getExposureDaysInternal() {
		List<ExposureDay> list =
				Json.safeFromJson(esp.getString(PREF_KEY_EEXPOSURE_DAYS, "[]"), EXPOSUREDAY_LIST_TYPE, ArrayList::new);

		DayDate maxAgeForExposureDay = new DayDate().subtractDays(NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS);
		Iterator<ExposureDay> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (new DayDate(iterator.next().getReportDate()).isBefore(maxAgeForExposureDay)) {
				iterator.remove();
			}
		}

		Collections.sort(list, (a, b) -> a.getExposedDate().compareTo(b.getExposedDate()));

		return list;
	}

	public List<ExposureDay> getExposureDays() {
		List<ExposureDay> list = getExposureDaysInternal();
		Iterator<ExposureDay> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().isDeleted()) {
				iterator.remove();
			}
		}
		if (list.size() > 0) {
			ExposureDay lastDay = list.get(list.size() - 1);
			list = new ArrayList<>();
			list.add(lastDay);
		}
		return list;
	}


	public void addExposureDay(Context context, ExposureDay exposureDay) {
		List<ExposureDay> previousExposureDays = getExposureDaysInternal();
		for (ExposureDay previousExposureDay : previousExposureDays) {
			if (previousExposureDay.getExposedDate().equals(exposureDay.getExposedDate())) {
				return;//exposure day was already added
			}
		}

		int id = esp.getInt(PREF_KEY_LAST_ID, 0) + 1;
		exposureDay.setId(id);
		previousExposureDays.add(exposureDay);
		esp.edit()
				.putInt(PREF_KEY_LAST_ID, id)
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(previousExposureDays))
				.apply();

		BroadcastHelper.sendUpdateBroadcast(context);
	}

	public void resetExposureDays() {
		List<ExposureDay> previousExposureDays = getExposureDaysInternal();
		for (ExposureDay previousExposureDay : previousExposureDays) {
			previousExposureDay.setDeleted(true);
		}
		esp.edit()
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(previousExposureDays, EXPOSUREDAY_LIST_TYPE))
				.apply();
	}

	public void clear() {
		esp.edit()
				.putString(PREF_KEY_EEXPOSURE_DAYS, Json.toJson(new ArrayList<>(), EXPOSUREDAY_LIST_TYPE))
				.apply();
	}

}
