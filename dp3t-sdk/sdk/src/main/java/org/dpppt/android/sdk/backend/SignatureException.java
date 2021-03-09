/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.backend;

import java.io.IOException;

public class SignatureException extends IOException {

	public SignatureException(String message) {
		super(message);
	}

	public SignatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
