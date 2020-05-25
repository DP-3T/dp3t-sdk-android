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

import org.dpppt.android.sdk.internal.util.Json;

public class PendingKeyUploadStorage {

	private static final String PREF_KEY_PENDING_KEYS = "pendingKeys";

	private static PendingKeyUploadStorage instance;

	private SharedPreferences sp;

	public static synchronized PendingKeyUploadStorage getInstance(Context context) {
		if (instance == null) {
			instance = new PendingKeyUploadStorage(context);
		}
		return instance;
	}

	private PendingKeyUploadStorage(Context context) {
		try {
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
				sp = EncryptedSharedPreferences.create("dp3t_pendingkeyupload_store",
						KEY_ALIAS,
						context,
						EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
						EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
			} else {
				sp = context.getSharedPreferences("dp3t_pendingkeyupload_store_not_encrypted", Context.MODE_PRIVATE);
			}
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	private PendingKeyList getPendingKeys() {
		return Json.fromJson(sp.getString(PREF_KEY_PENDING_KEYS, "[]"), PendingKeyList.class);
	}

	private void setPendingKeys(PendingKeyList list) {
		Collections.sort(list, (a, b) -> Integer.compare(a.getRollingStartNumber(), b.getRollingStartNumber()));
		sp.edit().putString(PREF_KEY_PENDING_KEYS, Json.toJson(list)).apply();
	}

	public int peekRollingStartNumber() {
		PendingKeyList list = getPendingKeys();
		if (list.size() == 0) {
			return Integer.MAX_VALUE;
		} else {
			return list.get(0).getRollingStartNumber();
		}
	}

	public PendingKey popNextPendingKey() {
		synchronized (this) {
			PendingKeyList list = getPendingKeys();
			if (list.size() > 0) {
				PendingKey poped = list.remove(0);
				setPendingKeys(list);
				return poped;
			} else {
				return null;
			}
		}
	}

	public void addPendingKey(PendingKey key) {
		synchronized (this) {
			PendingKeyList list = getPendingKeys();
			list.add(key);
			setPendingKeys(list);
		}
	}

	public void clear() {
		sp.edit().clear().apply();
	}

	public static class PendingKey {
		private int rollingStartNumber;
		private String token;
		private int fake;

		public PendingKey(int rollingStartNumber, String token, int fake) {
			this.rollingStartNumber = rollingStartNumber;
			this.token = token;
			this.fake = fake;
		}

		public int getRollingStartNumber() {
			return rollingStartNumber;
		}

		public String getToken() {
			return token;
		}

		public boolean isFake() {
			return fake == 1;
		}

	}


	private static class PendingKeyList extends ArrayList<PendingKey> { }

}
