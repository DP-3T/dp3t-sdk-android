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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HistoryEntry entry = (HistoryEntry) o;

		if (successful != entry.successful) return false;
		if (time != entry.time) return false;
		if (type != entry.type) return false;
		return status != null ? status.equals(entry.status) : entry.status == null;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (successful ? 1 : 0);
		result = 31 * result + (int) (time ^ (time >>> 32));
		return result;
	}

}
