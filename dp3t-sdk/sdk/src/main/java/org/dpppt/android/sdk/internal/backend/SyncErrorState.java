/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import org.dpppt.android.sdk.TracingStatus.ErrorState;

public class SyncErrorState {

	private static SyncErrorState instance;

	private ErrorState syncError = null;

	private SyncErrorState() { }

	public static synchronized SyncErrorState getInstance() {
		if (instance == null) {
			instance = new SyncErrorState();
		}
		return instance;
	}

	public void setSyncError(ErrorState syncError) {
		this.syncError = syncError;
	}

	public ErrorState getSyncError() {
		return syncError;
	}

}
