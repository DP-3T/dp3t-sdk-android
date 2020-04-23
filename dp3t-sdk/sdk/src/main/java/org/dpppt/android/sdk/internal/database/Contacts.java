/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database;

interface Contacts {

	String TABLE_NAME = "contacts";

	String ID = "id";
	String DATE = "date";
	String EPHID = "ephid";
	String WINDOW_COUNT = "windowCount";
	String ASSOCIATED_KNOWN_CASE = "associated_known_case";

	String[] PROJECTION = {
			ID,
			DATE,
			EPHID,
			WINDOW_COUNT,
			ASSOCIATED_KNOWN_CASE
	};

	static String create() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				DATE + " INTEGER NOT NULL, " + EPHID + " BLOB NOT NULL, " +
				WINDOW_COUNT + " INTEGER NOT NULL, " + ASSOCIATED_KNOWN_CASE + " INTEGER, " +
				"CONSTRAINT no_duplicates UNIQUE (" + DATE + ", " + EPHID + "), " +
				"FOREIGN KEY (" + ASSOCIATED_KNOWN_CASE + ") REFERENCES " +
				KnownCases.TABLE_NAME + " (" + KnownCases.ID + ") ON DELETE SET NULL)";
	}

	static String drop() {
		return "DROP TABLE IF EXISTS " + TABLE_NAME;
	}

}
