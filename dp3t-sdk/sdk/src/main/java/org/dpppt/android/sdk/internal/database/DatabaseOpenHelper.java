/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

class DatabaseOpenHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "dp3t_sdk.db";

	private static DatabaseOpenHelper instance;

	static DatabaseOpenHelper getInstance(@NonNull Context context) {
		if (instance == null) {
			instance = new DatabaseOpenHelper(context);
		}
		return instance;
	}

	private DatabaseOpenHelper(@NonNull Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		recreateTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//empty for now
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db); //do as we create a new table
	}

	public void recreateTables(@NonNull SQLiteDatabase db) {
		new Transaction(db,
				Contacts.drop(),
				KnownCases.drop(),
				Handshakes.drop(),
				MatchedContacts.drop(),
				KnownCases.create(),
				Handshakes.create(),
				Contacts.create(),
				MatchedContacts.create()
		).run();
	}


	public void exportDatabaseTo(Context context, OutputStream targetOut) throws IOException {
		File db = context.getDatabasePath(DATABASE_NAME);
		FileInputStream fileInputStream = new FileInputStream(db);
		byte[] buf = new byte[2048];
		int len;
		while ((len = fileInputStream.read(buf)) > 0) {
			targetOut.write(buf, 0, len);
		}
		targetOut.close();
		fileInputStream.close();
	}

}
