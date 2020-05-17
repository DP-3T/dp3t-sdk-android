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


public class GaenKey {

	String keyData;

	int rollingStartNumber;

	int rollingPeriod;

	int transmissionRiskLevel;

	int fake;


	public GaenKey(String keyData, Integer rollingStartNumber, Integer rollingPeriod, Integer transmissionRiskLevel) {
		this(keyData, rollingStartNumber, rollingPeriod, transmissionRiskLevel, 0);
	}


	public GaenKey(String keyData, Integer rollingStartNumber, Integer rollingPeriod, Integer transmissionRiskLevel, int fake) {
		this.keyData = keyData;
		this.rollingStartNumber = rollingStartNumber;
		this.rollingPeriod = rollingPeriod;
		this.transmissionRiskLevel = transmissionRiskLevel;
		this.fake = fake;
	}

	public String getKeyData() {
		return this.keyData;
	}

	public Integer getRollingStartNumber() {
		return this.rollingStartNumber;
	}

	public Integer getRollingPeriod() {
		return this.rollingPeriod;
	}

	public Integer getTransmissionRiskLevel() {
		return this.transmissionRiskLevel;
	}

	public boolean isFake() {
		return fake == 1;
	}

}