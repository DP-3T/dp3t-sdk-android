/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal.crypto;

import java.util.Arrays;

public class EphId {

	private byte[] data;

	public EphId(byte[] data) {
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EphId ephId = (EphId) o;
		return Arrays.equals(data, ephId.data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

}
