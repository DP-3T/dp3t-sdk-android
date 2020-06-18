/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabase {

	private static HistoryDatabase database = null;

	private final HistoryDatabaseHelper dbHelper;

	public static synchronized HistoryDatabase getInstance(Context context) {
		if (database == null) {
			database = new HistoryDatabase(context);
		}
		return database;
	}

	private HistoryDatabase(Context context) {
		dbHelper = new HistoryDatabaseHelper(context);
	}

	public void addEntry(HistoryEntry entry) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		insert(db, entry);
	}

	protected static void insert(SQLiteDatabase db, HistoryEntry entry) {
		ContentValues values = new ContentValues();
		values.put(HistorySpec.COLUMN_NAME_TYPE, entry.getType().getId());
		values.put(HistorySpec.COLUMN_NAME_STATUS, entry.getStatus());
		values.put(HistorySpec.COLUMN_NAME_TIME, entry.getTime());
		values.put(HistorySpec.COLUMN_NAME_SUCCESS, entry.isSuccessful() ? 1 : 0);
		db.insert(HistorySpec.TABLE_NAME, null, values);
	}

	public List<HistoryEntry> getEntries() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String[] columns = new String[] { HistorySpec.COLUMN_NAME_TYPE, HistorySpec.COLUMN_NAME_STATUS,
				HistorySpec.COLUMN_NAME_SUCCESS, HistorySpec.COLUMN_NAME_TIME };

		Cursor cursor = db.query(HistorySpec.TABLE_NAME,
				columns,
				null,
				null,
				null,
				null,
				HistorySpec.COLUMN_NAME_TIME + " DESC");

		List<HistoryEntry> entries = new ArrayList<>();

		try {
			if (cursor.moveToFirst()) {
				int colIndType = cursor.getColumnIndex(HistorySpec.COLUMN_NAME_TYPE);
				int colIndStatus = cursor.getColumnIndex(HistorySpec.COLUMN_NAME_STATUS);
				int colIndSuccess = cursor.getColumnIndex(HistorySpec.COLUMN_NAME_SUCCESS);
				int colIndTime = cursor.getColumnIndex(HistorySpec.COLUMN_NAME_TIME);

				do {
					HistoryEntry entry = new HistoryEntry(
							HistoryEntryType.byId(cursor.getInt(colIndType)),
							cursor.getString(colIndStatus),
							cursor.getInt(colIndSuccess) != 0,
							cursor.getLong(colIndTime)
					);
					entries.add(entry);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		return entries;
	}

	public void clearBefore(long time) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		db.delete(HistorySpec.TABLE_NAME,
				HistorySpec.COLUMN_NAME_TIME + "< ?",
				new String[] { String.valueOf(time) });
		db.execSQL("VACUUM");
	}

	public void clear() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL("delete from " + HistorySpec.TABLE_NAME);
		db.execSQL("VACUUM");
		db.close();
	}


	public static class HistorySpec implements BaseColumns {

		public static final String TABLE_NAME = "history";
		static final String INDEX_NAME_TIME = "i_time";
		static final String COLUMN_NAME_TIME = "time";
		static final String COLUMN_NAME_TYPE = "type";
		static final String COLUMN_NAME_STATUS = "status";
		static final String COLUMN_NAME_SUCCESS = "success";

	}


	protected static class HistoryDatabaseHelper extends SQLiteOpenHelper {

		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "dp3t_history.db";

		private static final String SQL_CREATE_ENTRIES =
				"CREATE TABLE " + HistorySpec.TABLE_NAME + " (" +
						HistorySpec._ID + " INTEGER PRIMARY KEY," +
						HistorySpec.COLUMN_NAME_STATUS + " TEXT," +
						HistorySpec.COLUMN_NAME_TYPE + " INTEGER NOT NULL," +
						HistorySpec.COLUMN_NAME_SUCCESS + " INTEGER NOT NULL," +
						HistorySpec.COLUMN_NAME_TIME + " INTEGER NOT NULL)";

		private static final String SQL_CREATE_INDEX_TIME =
				"CREATE INDEX " + HistorySpec.INDEX_NAME_TIME + " ON " + HistorySpec.TABLE_NAME + "(" +
						HistorySpec.COLUMN_NAME_TIME + ")";

		HistoryDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			executeCreate(db);
		}

		protected static void executeCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_ENTRIES);
			db.execSQL(SQL_CREATE_INDEX_TIME);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

	}

}
