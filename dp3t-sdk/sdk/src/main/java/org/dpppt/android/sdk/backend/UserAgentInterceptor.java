package org.dpppt.android.sdk.backend;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {

	private final UserAgentGenerator userAgentGenerator;

	public UserAgentInterceptor(UserAgentGenerator userAgentGenerator) {
		this.userAgentGenerator = userAgentGenerator;
	}

	@NonNull
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request()
				.newBuilder()
				.header("User-Agent", userAgentGenerator.getUserAgent())
				.build();
		return chain.proceed(request);
	}

	public interface UserAgentGenerator {

		String getUserAgent();

	}

}
