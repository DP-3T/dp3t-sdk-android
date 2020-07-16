/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.storage.models;

public class PendingKey {
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
