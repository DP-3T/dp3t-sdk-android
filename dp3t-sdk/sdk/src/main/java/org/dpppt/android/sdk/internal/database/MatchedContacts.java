/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database;

interface MatchedContacts {

	String TABLE_NAME = "matched_contacts";

	String ID = "id";
	String REPORT_DATE = "report_date";
	String ASSOCIATED_KNOWN_CASE = "known_case_id";

	String[] PROJECTION = {
			ID,
			REPORT_DATE,
			ASSOCIATED_KNOWN_CASE
	};

	static String create() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				REPORT_DATE + " INTEGER NOT NULL, " +
				ASSOCIATED_KNOWN_CASE + " INTEGER, " +
				"FOREIGN KEY (" + ASSOCIATED_KNOWN_CASE + ") REFERENCES " +
				KnownCases.TABLE_NAME + " (" + KnownCases.ID + ") ON DELETE SET NULL)";
	}

	static String drop() {
		return "DROP TABLE IF EXISTS " + TABLE_NAME;
	}

}
