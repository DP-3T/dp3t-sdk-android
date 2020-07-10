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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import org.dpppt.android.sdk.internal.storage.models.PendingKey;
import org.dpppt.android.sdk.internal.util.Json;

public class PendingKeyUploadStorage {

	private static final Type PENDINGKEY_LIST_TYPE = new TypeToken<LinkedList<PendingKey>>() { }.getType();

	private static final String PREF_KEY_PENDING_KEYS = "pendingKeys";

	private static PendingKeyUploadStorage instance;

	private SharedPreferences esp;

	public static synchronized PendingKeyUploadStorage getInstance(Context context) {
		if (instance == null) {
			instance = new PendingKeyUploadStorage(context);
		}
		return instance;
	}

	private PendingKeyUploadStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			esp = EncryptedSharedPreferences.create("dp3t_pendingkeyupload_store",
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<PendingKey> getPendingKeys() {
		return Json.fromJson(esp.getString(PREF_KEY_PENDING_KEYS, "[]"), PENDINGKEY_LIST_TYPE);
	}

	private void setPendingKeys(List<PendingKey> list) {
		Collections.sort(list, (a, b) -> Integer.compare(a.getRollingStartNumber(), b.getRollingStartNumber()));
		esp.edit().putString(PREF_KEY_PENDING_KEYS, Json.toJson(list, PENDINGKEY_LIST_TYPE)).apply();
	}

	public int peekRollingStartNumber() {
		List<PendingKey> list = getPendingKeys();
		if (list.size() == 0) {
			return Integer.MAX_VALUE;
		} else {
			return list.get(0).getRollingStartNumber();
		}
	}

	public synchronized PendingKey popNextPendingKey() {
		List<PendingKey> list = getPendingKeys();
		if (list.size() > 0) {
			PendingKey popped = list.remove(0);
			setPendingKeys(list);
			return popped;
		} else {
			return null;
		}
	}

	public synchronized void addPendingKey(PendingKey key) {
		List<PendingKey> list = getPendingKeys();
		list.add(key);
		setPendingKeys(list);
	}

	public void clear() {
		esp.edit().clear().apply();
	}

}
