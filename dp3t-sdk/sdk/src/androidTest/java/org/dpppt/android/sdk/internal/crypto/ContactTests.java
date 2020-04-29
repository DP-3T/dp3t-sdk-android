package org.dpppt.android.sdk.internal.crypto;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.dpppt.android.sdk.internal.backend.models.ExposeeRequest;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.database.models.Handshake;
import org.dpppt.android.sdk.internal.util.DayDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.dpppt.android.sdk.internal.util.Base64Util.fromBase64;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ContactTests {

	private static final long MINUTE = 60 * 1000l;
	private static final long DAY = 24 * 60 * 60 * 1000l;

	@Test
	public void testExposureDateCreation() throws InterruptedException {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		CryptoModule module = CryptoModule.getInstance(context);
		module.reset();
		module.init();

		Database database = new Database(context);
		database.recreateTablesSynchronous();

		for (int offset = -30; offset < -15; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() + offset * MINUTE, module.getCurrentEphId(), -21, -70, "", "", 0));
		}

		database.generateContactsFromHandshakes(context);

		ExposeeRequest exposeeRequest = module.getSecretKeyForPublishing(new DayDate(), null);
		database.addKnownCase(context, fromBase64(exposeeRequest.getKey()), exposeeRequest.getKeyDate(),
				System.currentTimeMillis());

		waitForDatabase(database);
		assertTrue(database.getExposureDays().size() == 1);
	}


	@Test
	public void testExposureOnePersonTwoDays() throws InterruptedException {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		CryptoModule module = CryptoModule.getInstance(context);
		module.reset();
		module.init();

		Database database = new Database(context);
		database.recreateTablesSynchronous();

		byte[] sk0 = module.getCurrentSK(new DayDate());
		byte[] sk1 = module.getSKt1(sk0);

		for (int offset = -30; offset < -15; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() - DAY + offset * MINUTE, module.createEphIds(sk0, true).get(5),
							-21, -70, "", "", 0));
		}
		database.generateContactsFromHandshakes(context);

		for (int offset = -30; offset < -15; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() + offset * MINUTE, module.createEphIds(sk1, true).get(3),
							-21, -70, "", "", 0));
		}
		database.generateContactsFromHandshakes(context);

		database.addKnownCase(context, sk0, new DayDate(System.currentTimeMillis() - DAY).getStartOfDayTimestamp(),
				System.currentTimeMillis());

		waitForDatabase(database);
		assertTrue(database.getExposureDays().size() == 2);
	}

	@Test
	public void testLateDiscoveryOfKnownCase() throws InterruptedException {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		CryptoModule module = CryptoModule.getInstance(context);
		module.reset();
		module.init();

		Database database = new Database(context);
		database.recreateTablesSynchronous();
		int daysAgo = 11;

		for (int offset = -30; offset < -15; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() - daysAgo * DAY + offset * MINUTE, module.getCurrentEphId(), -21,
							-70,
							"", "", 0));
		}

		database.generateContactsFromHandshakes(context);

		database.addKnownCase(context, module.getCurrentSK(new DayDate()),
				new DayDate().subtractDays(daysAgo).getStartOfDayTimestamp(),
				System.currentTimeMillis());

		waitForDatabase(database);
		assertTrue(database.getExposureDays().size() == 0);
	}

	@Test
	public void testExposureFromMultipleUsers() throws InterruptedException {
		Context context = InstrumentationRegistry.getInstrumentation().getContext();
		CryptoModule module = CryptoModule.getInstance(context);
		module.reset();
		module.init();

		Database database = new Database(context);
		database.recreateTablesSynchronous();

		byte[] person0_sk0 = module.getCurrentSK(new DayDate());
		module.reset();
		module.init();
		byte[] person1_sk0 = module.getCurrentSK(new DayDate());

		for (int offset = -30; offset < -20; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() - DAY + offset * MINUTE,
							module.createEphIds(person0_sk0, true).get(5),
							-21, -70, "", "", 0));
		}
		database.generateContactsFromHandshakes(context);

		for (int offset = -30; offset < -20; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() - DAY + offset * MINUTE,
							module.createEphIds(person1_sk0, true).get(5),
							-21, -70, "", "", 0));
		}
		database.generateContactsFromHandshakes(context);

		byte[] person1_sk1 = module.getSKt1(person1_sk0);
		for (int offset = -30; offset < -15; offset++) {
			database.addHandshake(context,
					new Handshake(0, System.currentTimeMillis() + offset * MINUTE, module.createEphIds(person1_sk1, true).get(5),
							-21, -70, "", "", 0));
		}
		database.generateContactsFromHandshakes(context);

		database.addKnownCase(context, person0_sk0,
				new DayDate().subtractDays(1).getStartOfDayTimestamp(),
				System.currentTimeMillis());

		waitForDatabase(database);
		assertTrue(database.getExposureDays().size() == 0);

		database.addKnownCase(context, person1_sk0,
				new DayDate().subtractDays(1).getStartOfDayTimestamp(),
				System.currentTimeMillis());

		waitForDatabase(database);
		assertTrue(database.getExposureDays().size() == 2);
	}


	private void waitForDatabase(Database database) throws InterruptedException {
		final Object syncObject = new Object();
		database.runOnDatabaseThread(() -> {
			synchronized (syncObject) {
				syncObject.notify();
			}
		});
		synchronized (syncObject) {
			syncObject.wait();
		}
	}

}
