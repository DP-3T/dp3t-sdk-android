/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.backend.models;

public class ApplicationInfo {

	private String appId;
	private String backendBaseUrl;

	public ApplicationInfo(String appId, String backendBaseUrl) {
		this.appId = appId;
		this.backendBaseUrl = backendBaseUrl;
	}

	public String getAppId() {
		return appId;
	}

	public String getBackendBaseUrl() {
		return backendBaseUrl;
	}

}
