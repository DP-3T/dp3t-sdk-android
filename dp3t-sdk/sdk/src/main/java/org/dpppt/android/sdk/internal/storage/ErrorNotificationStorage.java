package org.dpppt.android.sdk.internal.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.dpppt.android.sdk.internal.storage.models.ActiveNotificationErrors;
import org.dpppt.android.sdk.internal.util.Json;

public class ErrorNotificationStorage {

	private static final String PREF_KEY_ACTIVE_ERRORS = "active_errors";

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

}
