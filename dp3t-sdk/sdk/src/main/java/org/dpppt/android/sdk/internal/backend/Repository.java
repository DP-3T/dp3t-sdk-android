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

import android.content.Context;
import androidx.annotation.NonNull;

import java.net.Proxy;

import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.backend.UserAgentInterceptor;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

import static org.dpppt.android.sdk.internal.backend.ProxyConfig.DISABLE_SYSTEM_PROXY;

public interface Repository {

	default OkHttpClient.Builder getClientBuilder(@NonNull Context context) {
		OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();

		if (DP3T.getUserAgent() != null) {
			okHttpBuilder.addInterceptor(new UserAgentInterceptor(DP3T.getUserAgent()));
		}

		int cacheSize = 50 * 1024 * 1024; // 50 MB
		Cache cache = new Cache(context.getCacheDir(), cacheSize);
		okHttpBuilder.cache(cache);

		okHttpBuilder.certificatePinner(CertificatePinning.getCertificatePinner());

		if (DISABLE_SYSTEM_PROXY) {
			okHttpBuilder.proxy(Proxy.NO_PROXY);
		}

		return okHttpBuilder;
	}

}
