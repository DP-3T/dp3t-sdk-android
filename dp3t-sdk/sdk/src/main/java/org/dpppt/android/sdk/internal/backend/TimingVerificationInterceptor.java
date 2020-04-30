/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal.backend;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Response;

public class TimingVerificationInterceptor implements Interceptor {

	private static final long ALLOWED_SERVER_TIME_DIFF = 60 * 1000L;

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());

		Response networkResponse = response.networkResponse();
		Date serverTime = response.headers().getDate("Date");

		if (serverTime == null || networkResponse == null) {
			return response;
		}

		String ageString = response.header("Age");
		long age = ageString != null ? 1000 * Long.parseLong(ageString) : 0;
		long liveServerTime = serverTime.getTime() + age;

		if (Math.abs(networkResponse.receivedResponseAtMillis() - liveServerTime) > ALLOWED_SERVER_TIME_DIFF) {
			throw new ServerTimeOffsetException();
		}

		return response;
	}

}
