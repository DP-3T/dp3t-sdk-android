package org.dpppt.android.sdk.internal.history;

public class HistoryEntry {

	private HistoryEntryType type;
	private String status;
	private boolean successful;
	private long time;

	public HistoryEntry(HistoryEntryType type, String status, boolean successful, long time) {
		this.type = type;
		this.status = status;
		this.successful = successful;
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public HistoryEntryType getType() {
		return type;
	}

	public String getStatus() {
		return status;
	}

	public boolean isSuccessful() {
		return successful;
	}

}
