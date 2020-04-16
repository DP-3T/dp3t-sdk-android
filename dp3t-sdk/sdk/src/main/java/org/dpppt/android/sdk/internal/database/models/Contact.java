/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database.models;

import org.dpppt.android.sdk.internal.util.DayDate;

public class Contact {

	private int id;
	private DayDate date;
	private byte[] ephId;
	private int associatedKnownCase;

	public Contact(int id, DayDate date, byte[] ephId, int associatedKnownCase) {
		this.id = id;
		this.date = date;
		this.ephId = ephId;
		this.associatedKnownCase = associatedKnownCase;
	}

	public byte[] getEphId() {
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
