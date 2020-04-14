/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.util;

import android.util.Base64;

public class Base64Util {

	public static String toBase64(byte[] data) {
		return new String(Base64.encode(data, Base64.NO_WRAP));
	}

	public static byte[] fromBase64(String data) {
		return Base64.decode(data, Base64.NO_WRAP);
	}

}
