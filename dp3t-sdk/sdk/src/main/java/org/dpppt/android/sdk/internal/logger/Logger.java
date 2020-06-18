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

public final class Logger {

	private static LoggerImpl instance = null;
	private static LogLevel minLevel = LogLevel.OFF;

	public static void init(Context context, LogLevel level) {
		minLevel = level;
		instance = new LoggerImpl(context);
	}

	private Logger() { }

	public static void d(String tag, String message) {
		log(LogLevel.DEBUG, tag, message);
	}

	public static void i(String tag, String message) {
		log(LogLevel.INFO, tag, message);
	}

	public static void w(String tag, String message) {
		log(LogLevel.WARNING, tag, message);
	}

	public static void e(String tag, String message) {
		log(LogLevel.ERROR, tag, message);
	}

	public static void e(String tag, Throwable throwable) {
		if (instance == null || LogLevel.ERROR.getImportance() < minLevel.getImportance()) {
			return;
		}
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		log(LogLevel.ERROR, tag, sw.toString());
	}

	public static void e(String tag, String message, Throwable throwable) {
		if (instance == null || LogLevel.ERROR.getImportance() < minLevel.getImportance()) {
			return;
		}
		StringWriter sw = new StringWriter();
		sw.append(message).append(": ");
		throwable.printStackTrace(new PrintWriter(sw));
		log(LogLevel.ERROR, tag, sw.toString());
	}

	private static void log(LogLevel level, String tag, String message) {
		if (instance != null && level.getImportance() >= minLevel.getImportance()) {
			instance.log(level, tag, message);
		}
	}

	public static List<LogEntry> getLogs(long sinceTime) {
		if (instance != null) {
			return instance.getLogs(sinceTime);
		} else {
			return Collections.emptyList();
		}
	}

	public static void clear() {
		if (instance != null) {
			instance.clear();
		}
	}

	public static List<String> getTags() {
		if (instance != null) {
			return instance.getTags();
		} else {
			return Collections.emptyList();
		}
	}
}
