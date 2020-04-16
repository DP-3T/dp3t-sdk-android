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

import org.dpppt.android.sdk.internal.backend.models.ApplicationsList;
import retrofit2.Call;
import retrofit2.http.GET;

interface DiscoveryService {

	@GET("discovery_dev.json")
	Call<ApplicationsList> getDiscoveryDev();

	@GET("discovery.json")
	Call<ApplicationsList> getDiscovery();

}
