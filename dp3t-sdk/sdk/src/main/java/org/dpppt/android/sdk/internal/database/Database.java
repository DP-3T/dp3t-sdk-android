/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.database.models.Handshake;
import org.dpppt.android.sdk.internal.util.DayDate;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static org.dpppt.android.sdk.internal.util.Base64Util.fromBase64;

public class Database {

	private DatabaseOpenHelper databaseOpenHelper;
	private DatabaseThread databaseThread;

	public Database(@NonNull Context context) {
		databaseOpenHelper = DatabaseOpenHelper.getInstance(context);
		databaseThread = DatabaseThread.getInstance(context);
	}

	public void addKnownCase(Context context, @NonNull String key, @NonNull DayDate onsetDate, @NonNull DayDate bucketDate) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KnownCases.KEY, key);
		values.put(KnownCases.ONSET, onsetDate.getStartOfDayTimestamp());
		values.put(KnownCases.BUCKET_DAY, bucketDate.getStartOfDayTimestamp());
		databaseThread.post(() -> {

			long idOfAddedCase = db.insertWithOnConflict(KnownCases.TABLE_NAME, null, values, CONFLICT_IGNORE);

			if (idOfAddedCase == -1) {
				//key was already in the database, so we can ignore it
				return;
			}

			CryptoModule cryptoModule = CryptoModule.getInstance(context);
			cryptoModule.checkContacts(fromBase64(key), onsetDate, bucketDate, (date) -> getContacts(date), (contact) -> {
				ContentValues updateValues = new ContentValues();
				updateValues.put(Contacts.ASSOCIATED_KNOWN_CASE, idOfAddedCase);
				db.update(Contacts.TABLE_NAME, updateValues, Contacts.ID + "=" + contact.getId(), null);
				BroadcastHelper.sendUpdateBroadcast(context);
			});
		});
	}

	public void removeOldKnownCases() {
		databaseThread.post(() -> {
			SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
			DayDate lastDayToKeep = new DayDate().subtractDays(CryptoModule.NUMBER_OF_DAYS_TO_KEEP_DATA);
			db.delete(KnownCases.TABLE_NAME, KnownCases.BUCKET_DAY + " < ?",
					new String[] { "" + lastDayToKeep.getStartOfDayTimestamp() });
		});
	}

	public void addHandshake(Context context, byte[] star, int txPowerLevel, int rssi, long timestamp) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Handshakes.EPHID, star);
		values.put(Handshakes.TIMESTAMP, timestamp);
		values.put(Handshakes.TX_POWER_LEVEL, txPowerLevel);
		values.put(Handshakes.RSSI, rssi);
		databaseThread.post(() -> {
			db.insert(Handshakes.TABLE_NAME, null, values);
			BroadcastHelper.sendUpdateBroadcast(context);
		});
	}

	public List<Handshake> getHandshakes() {
		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		List<Handshake> handshakes = new ArrayList<>();
		try (Cursor cursor = db
				.query(Handshakes.TABLE_NAME, Handshakes.PROJECTION, null, null, null, null, Handshakes.ID)) {
			while (cursor.moveToNext()) {
				int id = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.ID));
				long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Handshakes.TIMESTAMP));
				byte[] star = cursor.getBlob(cursor.getColumnIndexOrThrow(Handshakes.EPHID));
				int txPowerLevel = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.TX_POWER_LEVEL));
				int rssi = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.RSSI));
				Handshake handShake = new Handshake(id, timestamp, star, txPowerLevel, rssi);
				handshakes.add(handShake);
			}
		}
		return handshakes;
	}

	public void getHandshakes(@NonNull ResultListener<List<Handshake>> resultListener) {
		databaseThread.post(new Runnable() {
			List<Handshake> handshakes = new ArrayList<>();

			@Override
			public void run() {
				handshakes = getHandshakes();
				databaseThread.onResult(() -> resultListener.onResult(handshakes));
			}
		});
	}

	public void generateContactsFromHandshakes(Context context) {
		databaseThread.post(() -> {

			long currentEpochStart = CryptoModule.getInstance(context).getCurrentEpochStart();

			List<Handshake> handshakes = getHandshakes();
			//TODO add advanced logic to create contacts
			for (Handshake handshake : handshakes) {
				Contact contact = new Contact(-1, new DayDate(handshake.getTimestamp()), handshake.getEphId(), 0);
				addContact(contact);
			}

			SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
			if (!BuildConfig.FLAVOR.equals("calibration")) {
				//unless in calibration mode, delete handshakes after converting them to contacts
				db.delete(Handshakes.TABLE_NAME, Handshakes.TIMESTAMP + " < ?",
						new String[] { "" + currentEpochStart });
			}
			DayDate lastDayToKeep = new DayDate().subtractDays(CryptoModule.NUMBER_OF_DAYS_TO_KEEP_DATA);
			db.delete(Contacts.TABLE_NAME, Contacts.DATE + " < ?", new String[] { "" + lastDayToKeep.getStartOfDayTimestamp() });
		});
	}

	private void addContact(Contact contact) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Contacts.EPHID, contact.getEphId());
		values.put(Contacts.DATE, contact.getDate().getStartOfDayTimestamp());
		db.insert(Contacts.TABLE_NAME, null, values);
	}

	public List<Contact> getContacts() {
		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db
				.query(Contacts.TABLE_NAME, Contacts.PROJECTION, null, null, null, null, Contacts.ID);
		return getContactsFromCursor(cursor);
	}

	public List<Contact> getContacts(DayDate dayDate) {
		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db
				.query(Contacts.TABLE_NAME, Contacts.PROJECTION, Contacts.DATE + "=?",
						new String[] { "" + dayDate.getStartOfDayTimestamp() }, null, null, Contacts.ID);
		return getContactsFromCursor(cursor);
	}

	private List<Contact> getContactsFromCursor(Cursor cursor) {
		List<Contact> contacts = new ArrayList<>();
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.ID));
			DayDate date = new DayDate(cursor.getLong(cursor.getColumnIndexOrThrow(Contacts.DATE)));
			byte[] ephid = cursor.getBlob(cursor.getColumnIndexOrThrow(Contacts.EPHID));
			int associatedKnownCase = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.ASSOCIATED_KNOWN_CASE));
			Contact contact = new Contact(id, date, ephid, associatedKnownCase);
			contacts.add(contact);
		}
		return contacts;
	}

	public boolean wasContactExposed() {
		for (Contact contact : getContacts()) {
			if (contact.getAssociatedKnownCase() != 0) {
				return true;
			}
		}
		return false;
	}

	public void recreateTables(ResultListener<Void> listener) {
		databaseThread.post(() -> {
			databaseOpenHelper.recreateTables(databaseOpenHelper.getWritableDatabase());
			listener.onResult(null);
		});
	}

	public void exportTo(Context context, OutputStream targetOut, ResultListener<Void> listener) {
		databaseThread.post(() -> {
			try {
				databaseOpenHelper.exportDatabaseTo(context, targetOut);
			} catch (IOException e) {
				e.printStackTrace();
			}
			listener.onResult(null);
		});
	}

}
