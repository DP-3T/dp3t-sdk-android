package org.dpppt.android.sdk.internal.backend.models;

public class CachedResult<T> {

	private T data;
	private boolean isFromCache;

	public CachedResult(T data, boolean isFromCache) {
		this.data = data;
		this.isFromCache = isFromCache;
	}

	public T getData() {
		return data;
	}

	public boolean isFromCache() {
		return isFromCache;
	}

}
