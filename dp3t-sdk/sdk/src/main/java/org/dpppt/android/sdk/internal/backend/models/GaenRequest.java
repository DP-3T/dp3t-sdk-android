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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import org.dpppt.android.sdk.util.DateUtil;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class GaenRequest {
	List<GaenKey> gaenKeys;

	int delayedKeyDate;

	int fake;

	public GaenRequest(List<TemporaryExposureKey> temporaryExposureKeys, int delayedKeyDate) {
		ArrayList<GaenKey> keys = new ArrayList<>();
		int rollingStartNumber = DateUtil.getCurrentRollingStartNumber();
		for (TemporaryExposureKey temporaryExposureKey : temporaryExposureKeys) {
			keys.add(new GaenKey(toBase64(temporaryExposureKey.getKeyData()),
					temporaryExposureKey.getRollingStartIntervalNumber(),
					temporaryExposureKey.getRollingPeriod(),
					temporaryExposureKey.getTransmissionRiskLevel()));
			rollingStartNumber = Math.min(rollingStartNumber, temporaryExposureKey.getRollingStartIntervalNumber());
		}
		SecureRandom random = new SecureRandom();
		while (keys.size() < 30) {
			byte[] bytes = new byte[16];
			random.nextBytes(bytes);
			rollingStartNumber = rollingStartNumber - 144;
			keys.add(new GaenKey(toBase64(bytes),
					rollingStartNumber,
					144,
					0,
					1));
		}

		this.gaenKeys = keys;
		this.delayedKeyDate = delayedKeyDate;
		this.fake = 0;
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