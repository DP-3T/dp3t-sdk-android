/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
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
