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

public class Exposee {

	private String key;

	private DayDate onset;

	public Exposee(String key, DayDate onset) {
		this.key = key;
		this.onset = onset;
	}

	public String getKey() {
		return key;
	}

	public DayDate getOnset() {
		return onset;
	}

}
