package org.dpppt.android.sdk.internal.backend;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Response;

public class TimingVerificationInterceptor implements Interceptor {

	private static final long ALLOWED_SERVER_TIME_DIFF = 30 * 1000L;

	@NonNull
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());

		Response networkResponse = response.networkResponse();
		Date serverTime = response.headers().getDate("Date");
		if (networkResponse != null && serverTime != null &&
				Math.abs(networkResponse.receivedResponseAtMillis() - serverTime.getTime()) > ALLOWED_SERVER_TIME_DIFF) {
			throw new ServerTimeOffsetException();
		}

		return response;
	}

}
