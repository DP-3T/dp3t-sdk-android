/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database;

interface ExposureDays {

	String TABLE_NAME = "exposure_days";

	String ID = "id";
	String EXPOSED_DATE = "exposed_date";
	String REPORT_DATE = "report_date";

	String[] PROJECTION = {
			ID,
			EXPOSED_DATE,
			REPORT_DATE
	};

	static String create() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				EXPOSED_DATE + " INTEGER NOT NULL, " +
				REPORT_DATE + " INTEGER NOT NULL, " +
				"UNIQUE (" + EXPOSED_DATE + ") )";
	}

	static String drop() {
		return "DROP TABLE IF EXISTS " + TABLE_NAME;
	}

}
