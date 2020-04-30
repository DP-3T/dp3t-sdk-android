/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper {

	public static SQLiteDatabase getWritableDatabase(Context context) {
		return DatabaseOpenHelper.getInstance(context).getWritableDatabase();
	}

}
