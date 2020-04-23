/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.crypto;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.dpppt.android.sdk.internal.util.Base64Util.fromBase64;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CryptoTest {

	@Test
	public void generateEphIds() {
		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		module.init();
		List<EphId> allEphIdsOfToday = module.getEphIdsForToday(new DayDate());
		EphId currentEphId = module.getCurrentEphId();
		int matchingCount = 0;
		for (EphId ephId : allEphIdsOfToday) {
			if (ephId.equals(currentEphId)) {
				matchingCount++;
			}
		}
		assertTrue(matchingCount == 1);
	}

	@Test
	public void testStableEphIdsForDay() {
		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		module.init();
		List<EphId> allEphIdsOfToday = module.getEphIdsForToday(new DayDate());
		List<EphId> allEphIdsOfToday2 = module.getEphIdsForToday(new DayDate());

		for (int i = 0; i < allEphIdsOfToday.size(); i++) {
			assertTrue(allEphIdsOfToday.get(i).equals(allEphIdsOfToday2.get(i)));
		}
	}

	@Test
	public void testReset() {
		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		module.init();

		DayDate today = new DayDate();
		byte[] oldSecretKey = module.getCurrentSK(today);
		EphId oldCurrentEphId = module.getCurrentEphId();

		module.reset();
		module.init();

		byte[] newSecretKey = module.getCurrentSK(today);
		EphId mewCurrentEphId = module.getCurrentEphId();

		assertFalse(Arrays.equals(oldSecretKey, newSecretKey));
		assertFalse(oldCurrentEphId.equals(mewCurrentEphId));
	}

	@Test
	public void testTokenToday() {
		String key = "jZzsrFhswzLQlJDNnyvLotjoSTu4zZFAFXGUOfNA7Hw=";
		String token = "WoCgN+PiEk4mhKhMj6XD8w==";
		testKeyAndTokenToday(key, token, 1);
	}

	@Test
	public void testWrongTokenToday() {
		String key = "n5N07F0UnZ3DLWCpZ6rmQbWVYS1TDF/ttHLT8SdaHRs=";
		String token = "3daU4Ky04Zugx7RwRm7mQw==";
		testKeyAndTokenToday(key, token, 0);
	}

	private void testKeyAndTokenToday(String key, String token, int expectedCount) {
		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		module.init();

		EphId ephId = new EphId(fromBase64(token));
		DayDate today = new DayDate();
		List<Contact> contacts = new ArrayList<>();
		contacts.add(new Contact(0, today, ephId, 0));
		byte[] keyByte = fromBase64(key);

		HashSet<Contact> infectedContacts = new HashSet<>();
		module.checkContacts(keyByte, today.getStartOfDayTimestamp(), today.getStartOfDayTimestamp(),
				date -> contacts.stream().filter(c -> c.getDate().equals(date)).collect(Collectors.toList()),
				contact -> infectedContacts.add(contact));

		assertTrue(infectedContacts.size() == expectedCount);
	}

	@Test
	public void testTokenTodayWithYesterdaysKey() {
		String key = "n5N07F0UnZ3DLWCpZ6rmQbWVYS1TDF/ttHLT8SdaHRs=";
		String token = "ZN5cLwKOJVAWC7caIHskog==";

		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		module.init();

		EphId ephId = new EphId(fromBase64(token));
		DayDate today = new DayDate();
		DayDate yesterday = today.subtractDays(1);
		List<Contact> contacts = new ArrayList<>();
		contacts.add(new Contact(0, today, ephId, 0));
		byte[] keyByte = fromBase64(key);

		HashSet<Contact> infectedContacts = new HashSet<>();
		module.checkContacts(keyByte, yesterday.getStartOfDayTimestamp(), System.currentTimeMillis(),
				date -> contacts.stream().filter(c -> c.getDate().equals(date)).collect(Collectors.toList()),
				contact -> infectedContacts.add(contact));

		assertTrue(infectedContacts.size() == 1);
	}

}
