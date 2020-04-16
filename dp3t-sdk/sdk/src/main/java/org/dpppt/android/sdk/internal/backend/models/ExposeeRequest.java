/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.backend.models;

import org.dpppt.android.sdk.internal.util.DayDate;

public class ExposeeRequest {

	private String key;
	private DayDate onset;
	private ExposeeAuthData authData;

	public ExposeeRequest(String key, DayDate onset, ExposeeAuthData authData) {
		this.key = key;
		this.onset = onset;
		this.authData = authData;
	}

	public String getKey() {
		return key;
	}

	public DayDate getOnset() {
		return onset;
	}

	public ExposeeAuthData getAuthData() {
		return authData;
	}

}
