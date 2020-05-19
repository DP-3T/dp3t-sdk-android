/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.util;

import org.dpppt.android.sdk.models.DayDate;

public class DateUtil {

	public static int getCurrentRollingStartNumber() {
		return getRollingStartNumberForDate(System.currentTimeMillis());
	}

	public static int getRollingStartNumberForDate(long date) {
		return (int) (date / 1000 / 60 / 60 / 24) * 24 * 6;
	}

	public static int getRollingStartNumberForDate(DayDate onsetDate) {
		return getRollingStartNumberForDate(onsetDate.getStartOfDayTimestamp());
	}

}
