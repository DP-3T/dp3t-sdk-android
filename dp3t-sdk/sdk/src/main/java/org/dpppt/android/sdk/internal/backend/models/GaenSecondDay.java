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

public class GaenSecondDay {

	private GaenKey delayedKey;
	private int fake;

	public GaenSecondDay(GaenKey gaenKey) {
		this.delayedKey = gaenKey;
		this.fake = gaenKey.fake;
	}

	public Integer isFake() {
		return this.fake;
	}

	public GaenKey getDelayedKey() {
		return delayedKey;
	}

}
