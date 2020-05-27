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

import java.util.List;

class LoggerImpl {

	private final LogDatabase database;

	LoggerImpl(Context context) {
		this.database = new LogDatabase(context);
	}

	void log(LogLevel level, String tag, String message) {
		level.getLogcat().log(tag, message);

		database.log(level.getKey(), tag, message, System.currentTimeMillis());
	}

	List<LogEntry> getLogs(long sinceTime) {
		return database.getLogsSince(sinceTime);
	}

	void clear() {
		database.clear();
	}

	List<String> getTags() {
		return database.getTags();
	}

}
