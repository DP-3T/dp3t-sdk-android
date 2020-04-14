/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.logger;

public enum LogLevel {

	DEBUG("d", "debug", 1),
	INFO("i", "info", 2),
	ERROR("e", "error", 3),
	OFF("-", "off", Integer.MAX_VALUE);

	private final String key;
	private final String value;
	private final int i;

	LogLevel(String key, String value, int i) {
		this.key = key;
		this.value = value;
		this.i = i;
	}

	public static LogLevel byKey(String key) {
		for (LogLevel value : LogLevel.values()) {
			if (value.getKey().equals(key)) {
				return value;
			}
		}
		return null;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public int getI() {
		return i;
	}
}
