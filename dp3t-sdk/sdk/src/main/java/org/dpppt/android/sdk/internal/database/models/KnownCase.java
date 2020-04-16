/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database.models;

import static org.dpppt.android.sdk.internal.util.Base64Util.fromBase64;

public class KnownCase {

	private int id;
	private String day;
	private String key;

	public KnownCase(int id, String day, String key) {
		this.id = id;
		this.day = day;
		this.key = key;
	}

	public int getId() {
		return id;
	}

	public String getDay() {
		return day;
	}

	public String getKey() {
		return key;
	}

	public byte[] getParsedKey() {
		return fromBase64(key);
	}

}
