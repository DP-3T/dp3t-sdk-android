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

import java.util.ArrayList;
import java.util.List;

public class ApplicationsList {

	private List<ApplicationInfo> applications;

	public ApplicationsList() {
		applications = new ArrayList<>();
	}

	public List<ApplicationInfo> getApplications() {
		return applications;
	}

}
