/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal;

import android.util.Log;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AESBenchmark {

	@Parameterized.Parameters
	public static Collection<Integer> data() {
		return Arrays.asList(100, 1000, 10000, 100000);
	}

	private int size;

	public AESBenchmark(int size) {
		this.size = size;
	}

	@Rule
	public BenchmarkRule benchmarkRule = new BenchmarkRule();

	@Test
	public void micro_create_ephIDs() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {

		BenchmarkState state = benchmarkRule.getState();

		byte[] SKt = new byte[32];
		new Random().nextBytes(SKt);
		byte[] broadcast_key = "BLUBBLUBBLUBBLUBBLUBBLUBBLUBBLUB".getBytes();

		while (state.keepRunning()) {
			long start = System.currentTimeMillis();
			create_ephIDs(SKt, broadcast_key, size);
			long end = System.currentTimeMillis();
			Log.d("AESBenchmark", String.format("Creating " + size + " ephIDs took %.3f seconds.", (end - start) / 1000.0));
		}
	}

	private ArrayList<byte[]> create_ephIDs(byte[] SKt, byte[] broadcast_key, int epochs)
			throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SKt, "HmacSHA256"));
		mac.update(broadcast_key);
		byte[] keyBytes = mac.doFinal();

		byte[] emptyArray = new byte[16];

		//generate EphIDs
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, new SecureRandom());
		ArrayList<byte[]> emphIds = new ArrayList<>();
		for (int i = 0; i < epochs; i++) {
			emphIds.add(cipher.update(emptyArray));
		}
		return emphIds;
	}

}
