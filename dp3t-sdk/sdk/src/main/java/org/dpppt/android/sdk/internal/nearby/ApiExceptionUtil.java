/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;

public class ApiExceptionUtil {

	private static final Pattern CONNECTION_RESULT_PATTERN =
			Pattern.compile("ConnectionResult\\{[^}]*statusCode=[a-zA-Z0-9_]+\\((\\d+)\\)");

	public static final int UNKNOWN_STATUS_CODE = -2;

	public static int getENApiStatusCode(ApiException apiException) {
		Status status = apiException.getStatus();
		String statusMessage = status.getStatusMessage();
		if (statusMessage != null) {
			Matcher matcher = CONNECTION_RESULT_PATTERN.matcher(statusMessage);
			if (matcher.find()) {
				String connectionStatusCode = matcher.group(1);
				return Integer.parseInt(connectionStatusCode);
			}
		}
		return UNKNOWN_STATUS_CODE;
	}

}
