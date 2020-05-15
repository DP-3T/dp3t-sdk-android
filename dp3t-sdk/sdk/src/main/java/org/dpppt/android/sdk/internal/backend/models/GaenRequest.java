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

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.util.DateUtil;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class GaenRequest {
	List<GaenKey> gaenKeys;

	int delayedKeyDate;

	int fake;

	public GaenRequest(List<TemporaryExposureKey> temporaryExposureKeys) {
		ArrayList<GaenKey> keys = new ArrayList<>();
		for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
			keys.add(new GaenKey(toBase64(temporaryExposureKey.getKeyData()),
					temporaryExposureKey.getRollingStartIntervalNumber(),
					temporaryExposureKey.getRollingPeriod(),
					temporaryExposureKey.getTransmissionRiskLevel()));
		}
		while (keys.size() < 14) {
			keys.add(new GaenKey(toBase64(new byte[16]),
					DateUtil.getCurrentRollingStartNumber(),
					0,
					0));
		}

		this.gaenKeys = keys;
		this.delayedKeyDate = DateUtil.getCurrentRollingStartNumber();
		this.fake = 0;
	}

	public GaenRequest(List<GaenKey> gaenKeys, int delayedKeyDate) {
		this(gaenKeys, delayedKeyDate, 0);
	}

	public GaenRequest(List<GaenKey> gaenKeys, int delayedKeyDate, int fake) {
		this.gaenKeys = gaenKeys;
		this.delayedKeyDate = delayedKeyDate;
		this.fake = fake;
	}

	public List<GaenKey> getGaenKeys() {
		return this.gaenKeys;
	}

	public void setGaenKeys(List<GaenKey> gaenKeys) {
		this.gaenKeys = gaenKeys;
	}

	public Integer isFake() {
		return this.fake;
	}

	public void setFake(Integer fake) {
		this.fake = fake;
	}

}