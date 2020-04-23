package org.dpppt.android.sdk.internal.crypto;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import org.dpppt.android.sdk.internal.database.DatabaseHelper;
import org.dpppt.android.sdk.internal.util.DayDate;

public class CryptoDatabaseHelper {

	private static final String TABLE_NAME = "calibration_secret_keys";
	private static final String KEY = "key";
	private static final String DATE = "date";

	public static void copySKsToDatabase(Context context) {
		SQLiteDatabase database = DatabaseHelper.getWritableDatabase(context);
		database.beginTransaction();
		database.execSQL("drop table if exists " + TABLE_NAME);
		database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + KEY + " BLOB NOT NULL, " +
				DATE + " INTEGER NOT NULL)");
		for (Pair<DayDate, byte[]> keyPair : CryptoModule.getInstance(context).getSKList()) {
			ContentValues insertValues = new ContentValues();
			insertValues.put(KEY, keyPair.second);
			insertValues.put(DATE, keyPair.first.getStartOfDayTimestamp());
			database.insert(TABLE_NAME, null, insertValues);
		}
		database.setTransactionSuccessful();
		database.endTransaction();
	}

}
