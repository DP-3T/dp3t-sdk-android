package org.dpppt.android.sdk.internal.history;

import android.util.SparseArray;

public enum HistoryEntryType {
	OPEN_APP(0),
	SYNC(1),
	WORKER_STARTED(2),
	FAKE_REQUEST(3),
	NEXT_DAY_KEY_UPLOAD_REQUEST(4);

	private static final SparseArray<HistoryEntryType> ID_TYPE_MAP = new SparseArray<>(HistoryEntryType.values().length);

	static {
		for (HistoryEntryType type : HistoryEntryType.values()) {
			ID_TYPE_MAP.put(type.id, type);
		}
	}

	private int id;

	HistoryEntryType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static HistoryEntryType byId(int id) {
		HistoryEntryType type = ID_TYPE_MAP.get(id);
		if (type == null) throw new IllegalArgumentException("Invalid Type ID: " + id);
		return type;
	}
}
