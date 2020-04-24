/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.backend;

public interface ResponseCallback<T> {

	void onSuccess(T response);

	void onError(Throwable throwable);
}
