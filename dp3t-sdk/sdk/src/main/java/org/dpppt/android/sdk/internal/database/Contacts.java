/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database;

interface Contacts {

	String TABLE_NAME = "contacts";

	String ID = "id";
	String DATE = "date";
	String EPHID = "ephid";
	String ASSOCIATED_KNOWN_CASE = "associated_known_case";

	String[] PROJECTION = {
			ID,
			DATE,
			EPHID,
			ASSOCIATED_KNOWN_CASE
	};

	static String create() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				DATE + " INTEGER NOT NULL, " + EPHID + " BLOB NOT NULL, " +
				ASSOCIATED_KNOWN_CASE + " INTEGER, FOREIGN KEY (" + ASSOCIATED_KNOWN_CASE + ") REFERENCES " +
				KnownCases.TABLE_NAME + " (" + KnownCases.ID + ") ON DELETE SET NULL)";
	}

	static String drop() {
		return "DROP TABLE IF EXISTS " + TABLE_NAME;
	}

}
