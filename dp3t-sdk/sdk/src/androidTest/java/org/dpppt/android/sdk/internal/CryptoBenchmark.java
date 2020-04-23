/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;

import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.crypto.EphId;
import org.dpppt.android.sdk.internal.database.models.Contact;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CryptoBenchmark {

	private static final int NUMBER_OF_DAYS_TO_TEST = 5;
	private static final int NUMBER_OF_CONTACTS_PER_DAY = 50;

	@Rule
	public BenchmarkRule benchmarkRule = new BenchmarkRule();

	@Test
	public void key_matching_performance() {
		final BenchmarkState state = benchmarkRule.getState();
		CryptoModule module = CryptoModule.getInstance(InstrumentationRegistry.getInstrumentation().getContext());
		module.reset();
		if (module.init()) {
			int i = 0;
			while (state.keepRunning()) {
				String key = "much longer key which is used for the hash functino but this should not have an influence" +
						Integer.toHexString(i);
				module.checkContacts(key.getBytes(), new DayDate().subtractDays(NUMBER_OF_DAYS_TO_TEST), System.currentTimeMillis(), (date -> {
					ArrayList<Contact> contacts = new ArrayList<>();
					for (int x = 0; x < NUMBER_OF_CONTACTS_PER_DAY; x++) {
						contacts.add(new Contact(0, new DayDate(), new EphId(new byte[CryptoModule.KEY_LENGTH]), 0));
					}
					return contacts;
				}), (contact -> {}));
				i += 1;
			}
		}
	}

}
