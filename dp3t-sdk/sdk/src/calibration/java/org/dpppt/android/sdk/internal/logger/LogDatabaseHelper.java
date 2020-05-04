/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.dpppt.android.sdk.internal.database.DatabaseHelper;

public class LogDatabaseHelper {

	public static void copyLogDatabase(Context context) {
		SQLiteDatabase database = DatabaseHelper.getWritableDatabase(context);
		database.beginTransaction();
		database.execSQL("drop table if exists " + LogDatabase.LogSpec.TABLE_NAME);
		LogDatabase.LogDatabaseHelper.executeCreate(database);
		for (LogEntry logEntry : Logger.getLogs(0)) {
			LogDatabase
					.insert(database, logEntry.getLevel().getKey(), logEntry.getTag(), logEntry.getMessage(), logEntry.getTime());
		}
		database.setTransactionSuccessful();
		database.endTransaction();
	}

}
