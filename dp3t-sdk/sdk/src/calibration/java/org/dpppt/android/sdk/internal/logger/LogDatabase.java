/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import org.dpppt.android.sdk.BuildConfig;

class LogDatabase {

	private final LogDatabaseHelper dbHelper;

	LogDatabase(Context context) {
		dbHelper = new LogDatabaseHelper(context);
	}

	void log(String level, String tag, String message, long time) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(LogSpec.COLUMN_NAME_VERSION, BuildConfig.VERSION_CODE);
		values.put(LogSpec.COLUMN_NAME_LEVEL, level);
		values.put(LogSpec.COLUMN_NAME_TAG, tag);
		values.put(LogSpec.COLUMN_NAME_MESSAGE, message);
		values.put(LogSpec.COLUMN_NAME_TIME, time);
		db.insert(LogSpec.TABLE_NAME, null, values);
	}

	List<LogEntry> getLogsSince(long sinceTime) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String[] cols = new String[] { LogSpec.COLUMN_NAME_TIME, LogSpec.COLUMN_NAME_LEVEL,
				LogSpec.COLUMN_NAME_TAG, LogSpec.COLUMN_NAME_MESSAGE };
		Cursor cursor = db.query(LogSpec.TABLE_NAME,
				cols,
				LogSpec.COLUMN_NAME_TIME + ">=?",
				new String[] { String.valueOf(sinceTime) },
				null,
				null,
				LogSpec.COLUMN_NAME_TIME + " ASC");

		List<LogEntry> logEntries = new ArrayList<>();

		try {
			if (cursor.moveToFirst()) {
				int colIdxTime = cursor.getColumnIndex(LogSpec.COLUMN_NAME_TIME);
				int colIdxLevel = cursor.getColumnIndex(LogSpec.COLUMN_NAME_LEVEL);
				int colIdxTag = cursor.getColumnIndex(LogSpec.COLUMN_NAME_TAG);
				int colIdxMessage = cursor.getColumnIndex(LogSpec.COLUMN_NAME_MESSAGE);

				do {
					LogEntry entry = new LogEntry(
							cursor.getLong(colIdxTime),
							LogLevel.byKey(cursor.getString(colIdxLevel)),
							cursor.getString(colIdxTag),
							cursor.getString(colIdxMessage)
					);
					logEntries.add(entry);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		return logEntries;
	}

	List<String> getTags() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		Cursor cursor = db.query(true,
				LogSpec.TABLE_NAME,
				new String[] { LogSpec.COLUMN_NAME_TAG },
				null,
				null,
				null,
				null,
				LogSpec.COLUMN_NAME_TAG + " ASC",
				null);

		List<String> tags = new ArrayList<>();

		try {
			if (cursor.moveToFirst()) {
				int colIdxTag = cursor.getColumnIndex(LogSpec.COLUMN_NAME_TAG);
				do {
					tags.add(cursor.getString(colIdxTag));
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		return tags;
	}

	void clear() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL("delete from " + LogSpec.TABLE_NAME);
		db.execSQL("VACUUM");
		db.close();
	}


	private static class LogSpec implements BaseColumns {

		static final String TABLE_NAME = "log";
		static final String INDEX_NAME_LEVEL = "i_lvl";
		static final String INDEX_NAME_TAG = "i_tag";
		static final String INDEX_NAME_TIME = "i_time";
		static final String COLUMN_NAME_VERSION = "version";
		static final String COLUMN_NAME_LEVEL = "lvl";
		static final String COLUMN_NAME_TAG = "tag";
		static final String COLUMN_NAME_MESSAGE = "msg";
		static final String COLUMN_NAME_TIME = "time";

	}


	private static class LogDatabaseHelper extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 2;
		private static final String DATABASE_NAME = "dp3t_sdk_log.db";

		private static final String SQL_CREATE_ENTRIES =
				"CREATE TABLE " + LogSpec.TABLE_NAME + " (" +
						LogSpec._ID + " INTEGER PRIMARY KEY," +
						LogSpec.COLUMN_NAME_VERSION + " INTEGER NOT NULL," +
						LogSpec.COLUMN_NAME_LEVEL + " TEXT NOT NULL," +
						LogSpec.COLUMN_NAME_TAG + " TEXT NOT NULL," +
						LogSpec.COLUMN_NAME_MESSAGE + " TEXT NOT NULL," +
						LogSpec.COLUMN_NAME_TIME + " INTEGER NOT NULL)";

		private static final String SQL_CREATE_INDEX_LEVEL =
				"CREATE INDEX " + LogSpec.INDEX_NAME_LEVEL + " ON " + LogSpec.TABLE_NAME + "(" + LogSpec.COLUMN_NAME_LEVEL +
						")";
		private static final String SQL_CREATE_INDEX_TAG =
				"CREATE INDEX " + LogSpec.INDEX_NAME_TAG + " ON " + LogSpec.TABLE_NAME + "(" + LogSpec.COLUMN_NAME_TAG + ")";
		private static final String SQL_CREATE_INDEX_TIME =
				"CREATE INDEX " + LogSpec.INDEX_NAME_TIME + " ON " + LogSpec.TABLE_NAME + "(" + LogSpec.COLUMN_NAME_TIME + ")";

		private static final String SQL_UPDATE_2_ADD_VERSION_COLUMN =
				"ALTER TABLE " + LogSpec.TABLE_NAME + " ADD COLUMN " + LogSpec.COLUMN_NAME_VERSION + " INTEGER NOT NULL DEFAULT 1";

		LogDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_ENTRIES);
			db.execSQL(SQL_CREATE_INDEX_LEVEL);
			db.execSQL(SQL_CREATE_INDEX_TAG);
			db.execSQL(SQL_CREATE_INDEX_TIME);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				db.execSQL(SQL_UPDATE_2_ADD_VERSION_COLUMN);
			}
		}

	}

}
