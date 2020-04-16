/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.database.models;

import org.dpppt.android.sdk.internal.crypto.EphId;
import org.dpppt.android.sdk.internal.util.DayDate;

public class Contact {

	private int id;
	private DayDate date;
	private EphId ephId;
	private int associatedKnownCase;

	public Contact(int id, DayDate date, EphId ephId, int associatedKnownCase) {
		this.id = id;
		this.date = date;
		this.ephId = ephId;
		this.associatedKnownCase = associatedKnownCase;
	}

	public EphId getEphId() {
		return ephId;
	}

	public DayDate getDate() {
		return date;
	}

	public int getAssociatedKnownCase() {
		return associatedKnownCase;
	}

	public int getId() {
		return id;
	}

}
