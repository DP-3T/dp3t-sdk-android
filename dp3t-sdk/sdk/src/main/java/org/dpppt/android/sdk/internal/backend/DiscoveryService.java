/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
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
