/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.logger;

import android.content.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class Logger {


	public static void init(Context context, LogLevel level) {
	}

	public static void d(String tag, String message) {
		//ignore for production
	}

	public static void i(String tag, String message) {
		//ignore for production
	}

	public static void e(String tag, String message) {
		//ignore for production
	}

	public static void e(String tag, Throwable throwable) {
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		e(tag, sw.toString());
	}

	public static void clear() {
		//ignore for production
	}

	public static List<String> getTags() {
		//ignore for production
		return Collections.emptyList();
	}

}
