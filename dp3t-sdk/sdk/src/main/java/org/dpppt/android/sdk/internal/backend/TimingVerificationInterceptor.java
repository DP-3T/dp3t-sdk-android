/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.backend;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Date;

import org.dpppt.android.sdk.internal.logger.Logger;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Response;

public class TimingVerificationInterceptor implements Interceptor {

	private static final long ALLOWED_SERVER_TIME_DIFF = 10 * 60 * 1000L;

	private static final String TAG = "TimingVerification";

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());

		Response networkResponse = response.networkResponse();
		if (networkResponse == null) {
			return response;
		}

		Date serverTime = networkResponse.headers().getDate("Date");
		if (serverTime == null) {
			return response;
		}

		String ageString = networkResponse.header("Age");
		long age = ageString != null ? 1000 * Long.parseLong(ageString) : 0;
		long liveServerTime = serverTime.getTime() + age;

		if (Math.abs(networkResponse.receivedResponseAtMillis() - liveServerTime) > ALLOWED_SERVER_TIME_DIFF) {
			StringBuilder log = new StringBuilder(1111);
			log.append(networkResponse.toString()).append("\n");
			Headers headers = networkResponse.headers();
			for (int i = 0, count = headers.size(); i < count; i++) {
				log.append(headers.name(i)).append(": ").append(headers.value(i)).append("\n");
			}
			Logger.e(TAG, log.toString());

			throw new ServerTimeOffsetException();
		}

		return response;
	}

}
