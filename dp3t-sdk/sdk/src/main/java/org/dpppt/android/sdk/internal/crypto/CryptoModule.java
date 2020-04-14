/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

import org.dpppt.android.sdk.internal.backend.models.ExposeeAuthData;
import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.util.DayDate;

import static org.dpppt.android.sdk.internal.util.Base64Util.toBase64;

public class CryptoModule {

	public static final int KEY_LENGTH = 16;

	public static final int NUMBER_OF_DAYS_TO_KEEP_DATA = 21;
	private static final int NUMBER_OF_EPOCHS_PER_DAY = 24 * 12;
	private static final int MILLISECONDS_PER_EPOCH = 24 * 60 * 60 * 1000 / NUMBER_OF_EPOCHS_PER_DAY;
	private static final byte[] BROADCAST_KEY = "broadcast key".getBytes();


	private static final String KEY_SK_LIST_JSON = "SK_LIST_JSON";
	private static final String KEY_EPHIDS_TODAY_JSON = "EPHIDS_TODAY_JSON";
	private static CryptoModule instance;
	private SharedPreferences esp;

	public static CryptoModule getInstance(Context context) {
		if (instance == null) {
			instance = new CryptoModule();
			try {
				String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
				instance.esp = EncryptedSharedPreferences.create("dp3t_store",
						KEY_ALIAS,
						context,
						EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
						EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
			} catch (GeneralSecurityException | IOException ex) {
				ex.printStackTrace();
			}
		}
		return instance;
	}

	public boolean init() {
		try {
			String stringKey = esp.getString(KEY_SK_LIST_JSON, null);
			if (stringKey != null) return true; //key already exists

			KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
			SecretKey secretKey = keyGenerator.generateKey();
			SKList skList = new SKList();
			skList.add(new Pair(new DayDate(System.currentTimeMillis()), secretKey.getEncoded()));
			storeSKList(skList);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private SKList getSKList() {
		String skListJson = esp.getString(KEY_SK_LIST_JSON, null);
		return new Gson().fromJson(skListJson, SKList.class);
	}

	private void storeSKList(SKList skList) {
		esp.edit().putString(KEY_SK_LIST_JSON, new Gson().toJson(skList)).commit();
	}

	protected byte[] getSKt1(byte[] SKt0) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] SKt1 = digest.digest(SKt0);
			return SKt1;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm must be present!");
		}
	}

	private void rotateSK() {
		SKList skList = getSKList();
		DayDate nextDay = skList.get(0).first.getNextDay();
		byte[] SKt1 = getSKt1(skList.get(0).second);
		skList.add(0, new Pair(nextDay, SKt1));
		List subList = skList.subList(0, Math.min(NUMBER_OF_DAYS_TO_KEEP_DATA, skList.size()));
		skList = new SKList();
		skList.addAll(subList);
		storeSKList(skList);
	}

	protected byte[] getCurrentSK(DayDate day) {
		SKList SKList = getSKList();
		while (SKList.get(0).first.isBefore(day)) {
			rotateSK();
			SKList = getSKList();
		}
		assert SKList.get(0).first.equals(day);
		return SKList.get(0).second;
	}

	protected List<byte[]> createEphIds(byte[] SK, boolean shuffle) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SK, "HmacSHA256"));
			mac.update(BROADCAST_KEY);
			byte[] prf = mac.doFinal();

			//generate EphIDs
			SecretKeySpec keySpec = new SecretKeySpec(prf, "AES");
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			byte[] counter = new byte[16];
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(counter));
			ArrayList<byte[]> ephIds = new ArrayList<>();
			byte[] emptyArray = new byte[KEY_LENGTH];
			for (int i = 0; i < NUMBER_OF_EPOCHS_PER_DAY; i++) {
				ephIds.add(cipher.update(emptyArray));
			}
			if (shuffle) {
				Collections.shuffle(ephIds, new SecureRandom());
			}
			return ephIds;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IllegalStateException("HmacSHA256 and AES algorithms must be present!", e);
		}
	}

	private int getEpochCounter(long time) {
		DayDate day = new DayDate(time);
		return (int) (time - day.getStartOfDayTimestamp()) / MILLISECONDS_PER_EPOCH;
	}

	public long getCurrentEpochStart() {
		long now = System.currentTimeMillis();
		DayDate currentDay = new DayDate(now);
		return currentDay.getStartOfDayTimestamp() + getEpochCounter(now) * MILLISECONDS_PER_EPOCH;
	}

	private EphIdsForDay getStoredEphIdsForToday() {
		String ephIdsJson = esp.getString(KEY_EPHIDS_TODAY_JSON, "null");
		return new Gson().fromJson(ephIdsJson, EphIdsForDay.class);
	}

	private void storeEphIdsForToday(EphIdsForDay ephIdsForDay) {
		esp.edit().putString(KEY_EPHIDS_TODAY_JSON, new Gson().toJson(ephIdsForDay)).commit();
	}

	protected List<byte[]> getEphIdsForToday(DayDate currentDay) {
		EphIdsForDay ephIdsForDay = getStoredEphIdsForToday();
		if (ephIdsForDay == null || !ephIdsForDay.dayDate.equals(currentDay)) {
			byte[] SK = getCurrentSK(currentDay);
			ephIdsForDay = new EphIdsForDay();
			ephIdsForDay.dayDate = currentDay;
			ephIdsForDay.ephIds = createEphIds(SK, true);
			storeEphIdsForToday(ephIdsForDay);
		}
		return ephIdsForDay.ephIds;
	}

	public byte[] getCurrentEphId() {
		long now = System.currentTimeMillis();
		DayDate currentDay = new DayDate(now);
		return getEphIdsForToday(currentDay).get(getEpochCounter(now));
	}

	public void checkContacts(byte[] sk, DayDate onsetDate, DayDate bucketDate, GetContactsCallback contactCallback,
			MatchCallback matchCallback) {

		DayDate dayToTest = onsetDate;
		byte[] skForDay = sk;
		while (dayToTest.isBeforeOrEquals(bucketDate)) {

			List<Contact> contactsOnDay = contactCallback.getContacts(dayToTest);
			if (contactsOnDay.size() > 0) {

				//generate all ephIds for day
				List<byte[]> ephIds = createEphIds(skForDay, false);

				//check all contacts if they match any of the ephIds
				for (Contact contact : contactsOnDay) {
					for (byte[] ephId : ephIds) {
						if (Arrays.equals(ephId, contact.getEphId())) {
							matchCallback.contactMatched(contact);
							break;
						}
					}
				}
			}

			//update day to next day and rotate sk accordingly
			dayToTest = dayToTest.getNextDay();
			skForDay = getSKt1(skForDay);
		}
	}

	public ExposeeRequest getSecretKeyForPublishing(DayDate date, ExposeeAuthData exposeeAuthData) {
		SKList skList = getSKList();
		for (Pair<DayDate, byte[]> daySKPair : skList) {
			if (daySKPair.first.equals(date)) {
				return new ExposeeRequest(
						toBase64(daySKPair.second),
						daySKPair.first,
						exposeeAuthData);
			}
		}
		if (date.isBefore(skList.get(skList.size() - 1).first)) {
			return new ExposeeRequest(
					toBase64(skList.get(skList.size() - 1).second),
					skList.get(skList.size() - 1).first,
					exposeeAuthData);
		}
		return null;
	}

	public void reset() {
		try {
			SharedPreferences.Editor editor = esp.edit();
			editor.clear();
			editor.commit();
			init();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public interface GetContactsCallback {

		List<Contact> getContacts(DayDate date);

	}


	public interface MatchCallback {

		void contactMatched(Contact contact);

	}

}
