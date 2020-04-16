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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.Response;

public class ResponseException extends Exception {

	private Response response;

	public ResponseException(@NonNull Response response) {
		this.response = response;
	}

	@Nullable
	@Override
	public String getMessage() {
		return "Code: " + response.code() + " Message: " + response.message();
	}
}
