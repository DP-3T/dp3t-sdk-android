/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class DummyTask<T> extends Task<T> {

	private static Executor executor = Executors.newSingleThreadExecutor();

	T value;

	public DummyTask(T value) {
		this.value = value;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public boolean isSuccessful() {
		return true;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public T getResult() {
		return value;
	}

	@Override
	public <X extends Throwable> T getResult(@NonNull Class<X> var1) throws X {
		return null;
	}

	@Nullable
	@Override
	public Exception getException() {
		return null;
	}

	@NonNull
	@Override
	public Task<T> addOnSuccessListener(@NonNull OnSuccessListener<? super T> var1) {
		executor.execute(() -> {
			var1.onSuccess(value);
		});
		return this;
	}

	@NonNull
	@Override
	public Task<T> addOnSuccessListener(@NonNull Executor var1, @NonNull OnSuccessListener<? super T> var2) {
		var1.execute(() -> {
			var2.onSuccess(value);
		});
		return this;
	}

	@NonNull
	@Override
	public Task<T> addOnSuccessListener(@NonNull Activity var1, @NonNull OnSuccessListener<? super T> var2) {
		var1.runOnUiThread(() -> {
			var2.onSuccess(value);
		});
		return this;
	}

	@NonNull
	@Override
	public Task<T> addOnFailureListener(@NonNull OnFailureListener var1) {
		return this;
	}

	@NonNull
	@Override
	public Task<T> addOnFailureListener(@NonNull Executor var1, @NonNull OnFailureListener var2) {
		return this;
	}

	@NonNull
	@Override
	public Task<T> addOnFailureListener(@NonNull Activity var1, @NonNull OnFailureListener var2) {
		return this;
	}

}
