package org.dpppt.android.sdk.internal.history;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class HistoryDbTest {

	private Context context;
	private HistoryDatabase db;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getContext();
		db = HistoryDatabase.getInstance(context);
	}

	@Test
	public void testClear() {
		db.clear();
		ArrayList<HistoryEntry> entries = new ArrayList<>();
		entries.add(new HistoryEntry(HistoryEntryType.SYNC, "AAB", true, 1591806623068L));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, 1591855343112L));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, 1591855355203L));
		for (HistoryEntry entry : entries) { db.addEntry(entry); }

		db.clear();

		List<HistoryEntry> addedEntries = db.getEntries();
		assertEquals(0, addedEntries.size());
	}

	@Test
	public void testInsertion() {
		db.clear();
		ArrayList<HistoryEntry> entries = new ArrayList<>();
		entries.add(new HistoryEntry(HistoryEntryType.SYNC, "AAB", true, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.FAKE_REQUEST, "BAA", false, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.FAKE_REQUEST, "AAB", true, System.currentTimeMillis()));
		entries.add(new HistoryEntry(HistoryEntryType.NEXT_DAY_KEY_UPLOAD_REQUEST, "AAA", false, System.currentTimeMillis()));
		for (HistoryEntry entry : entries) { db.addEntry(entry); }

		List<HistoryEntry> addedEntries = db.getEntries();
		assertEquals(entries.size(), addedEntries.size());
		for (int i = 0; i < entries.size(); i++) {
			assertEquals(entries.get(entries.size() - 1 - i), addedEntries.get(i));
		}
	}

	@Test
	public void testClearBefore() {
		db.clear();
		ArrayList<HistoryEntry> entries = new ArrayList<>();
		entries.add(new HistoryEntry(HistoryEntryType.SYNC, "AAB", true, 1591806623068L));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, 1591855343112L));
		entries.add(new HistoryEntry(HistoryEntryType.OPEN_APP, null, true, 1591855355203L));
		for (HistoryEntry entry : entries) { db.addEntry(entry); }

		db.clearBefore(1591848000000L);

		List<HistoryEntry> addedEntries = db.getEntries();
		assertEquals(2, addedEntries.size());
		for (int i = 0; i < 2; i++) {
			assertEquals(entries.get(entries.size() - 1 - i), (addedEntries.get(i)));
		}
	}

}
