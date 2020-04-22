package org.dpppt.android.sdk.internal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.dpppt.android.sdk.internal.logger.LogDatabase;
import org.dpppt.android.sdk.internal.logger.LogEntry;
import org.dpppt.android.sdk.internal.logger.Logger;

public class LogDatabaseHelper {

	public static void copyLogDatabase(Context context) {
		SQLiteDatabase database = DatabaseOpenHelper.getInstance(context).getWritableDatabase();
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
