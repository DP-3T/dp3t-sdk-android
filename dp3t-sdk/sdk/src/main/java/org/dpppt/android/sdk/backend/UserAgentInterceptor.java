package org.dpppt.android.sdk.backend;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {

	private final String userAgent;

	public UserAgentInterceptor(String userAgent) {
		this.userAgent = userAgent;
	}

	@NonNull
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request()
				.newBuilder()
				.header("User-Agent", userAgent)
				.build();
		return chain.proceed(request);
	}

}
