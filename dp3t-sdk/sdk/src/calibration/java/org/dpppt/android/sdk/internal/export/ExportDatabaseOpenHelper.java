/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.export;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExportDatabaseOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "export.db";

	private static ExportDatabaseOpenHelper instance;

	public static ExportDatabaseOpenHelper getInstance(@NonNull Context context) {
		if (instance == null) {
			instance = new ExportDatabaseOpenHelper(context);
		}
		return instance;
	}

	private ExportDatabaseOpenHelper(@NonNull Context context) {
		super(context, DATABASE_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public void exportDatabaseTo(Context context, OutputStream targetOut, Runnable successCallback,
			Consumer<Exception> errorCallback) {
		FileInputStream fileInputStream = null;
		try {
			File db = context.getDatabasePath(DATABASE_NAME);
			fileInputStream = new FileInputStream(db);
			byte[] buf = new byte[2048];
			int len;
			while ((len = fileInputStream.read(buf)) > 0) {
				targetOut.write(buf, 0, len);
			}
			targetOut.close();
			fileInputStream.close();
			successCallback.run();
		} catch (Exception e) {
			errorCallback.accept(e);
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
				targetOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
