package org.dpppt.android.sdk.internal.database.models;

public class MatchedContact {

	private int id;
	private long reportDate;

	public MatchedContact(int id, long reportDate) {
		this.id = id;
		this.reportDate = reportDate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getReportDate() {
		return reportDate;
	}

	public void setReportDate(long reportDate) {
		this.reportDate = reportDate;
	}

}
