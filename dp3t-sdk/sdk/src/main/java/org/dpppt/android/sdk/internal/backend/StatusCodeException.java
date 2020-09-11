/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class StatusCodeException extends Exception {

	private Response response;
	private String body;

	public StatusCodeException(@NonNull Response response, @Nullable ResponseBody errorBody) {
		this.response = response;
		this.body = readBody(errorBody);
	}

	@Nullable
	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder()
				.append("Code: ")
				.append(response.code())
				.append(" Message: ")
				.append(response.message());
		if (body != null) {
			sb.append(" Body: ").append(body);
		}
		return sb.toString();
	}

	@Nullable
	public String getBody() {
		return body;
	}

	public int getCode() {
		return response.code();
	}

	private static String readBody(ResponseBody body) {
		try {
			return body == null ? null : body.string();
		} catch (IOException error) {
			return null;
		}
	}
}
