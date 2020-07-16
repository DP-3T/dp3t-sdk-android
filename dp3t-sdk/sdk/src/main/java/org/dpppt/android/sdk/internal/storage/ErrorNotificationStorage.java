package org.dpppt.android.sdk.internal.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.reflect.TypeToken;

import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.internal.storage.models.ActiveNotificationErrors;
import org.dpppt.android.sdk.internal.util.Json;

public class ErrorNotificationStorage {

	private static final String PREF_KEY_ACTIVE_ERRORS = "active_errors";
	private static final String PREF_KEY_LAST_SHOWN_ERRORS = "last_shown_errors";

	private static final Type LAST_SHOWN_ERRORS_LIST_TYPE = new TypeToken<HashSet<String>>() { }.getType();


	private static ErrorNotificationStorage instance;

	private SharedPreferences esp;

	public static synchronized ErrorNotificationStorage getInstance(Context context) {
		if (instance == null) {
			instance = new ErrorNotificationStorage(context);
		}
		return instance;
	}

	private ErrorNotificationStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			esp = EncryptedSharedPreferences.create("dp3t_errornotification_store",
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ActiveNotificationErrors getSavedActiveErrors() {
		return Json.safeFromJson(esp.getString(PREF_KEY_ACTIVE_ERRORS, "{}"), ActiveNotificationErrors.class,
				ActiveNotificationErrors::new);
	}

	public void saveActiveErrors(ActiveNotificationErrors notificationErrors) {
		esp.edit().putString(PREF_KEY_ACTIVE_ERRORS, Json.toJson(notificationErrors, ActiveNotificationErrors.class)).apply();
	}

	public Set<ErrorState> getLastShownErrors() {
		Set<String> lastShownErrorKeys = Json.safeFromJson(esp.getString(PREF_KEY_LAST_SHOWN_ERRORS, "[]"),
				LAST_SHOWN_ERRORS_LIST_TYPE, HashSet::new);
		Set<ErrorState> lastShownErrors = new HashSet<>();
		for (String errorKey : lastShownErrorKeys) {
			ErrorState error = ErrorState.tryValueOf(errorKey);
			if (error != null) {
				lastShownErrors.add(error);
			}
		}
		return lastShownErrors;
	}

	public void saveLastShownErrors(Collection<ErrorState> lastShownErrors) {
		Set<String> lastShownErrorKeys = new HashSet<>();
		for (ErrorState error : lastShownErrors) {
			lastShownErrorKeys.add(error.name());
		}
		esp.edit().putString(PREF_KEY_LAST_SHOWN_ERRORS, Json.toJson(lastShownErrorKeys, LAST_SHOWN_ERRORS_LIST_TYPE)).apply();
	}

	public void clear() {
		esp.edit().clear().apply();
	}

}
