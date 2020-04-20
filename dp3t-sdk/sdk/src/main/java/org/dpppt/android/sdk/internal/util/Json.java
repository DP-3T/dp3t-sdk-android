package org.dpppt.android.sdk.internal.util;

import androidx.core.util.Supplier;

import com.google.gson.Gson;

public final class Json {

	private static final Gson GSON = new Gson();

	public static <T> String toJson(T object) {
		return GSON.toJson(object);
	}

	public static <T> T safeFromJson(String json, Class<T> classOfT, Supplier<T> defaultValue) {
		try {
			return GSON.fromJson(json, classOfT);
		} catch (Exception e) {
			return defaultValue.get();
		}
	}

	private Json() { }

}
