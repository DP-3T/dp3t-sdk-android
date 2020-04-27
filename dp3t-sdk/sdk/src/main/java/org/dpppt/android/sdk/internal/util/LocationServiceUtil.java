package org.dpppt.android.sdk.internal.util;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

public class LocationServiceUtil {

	public static Boolean isLocationEnabled(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			return lm != null && lm.isLocationEnabled();
		} else {
			int mode = Settings.Secure.getInt(context.getContentResolver(),
					Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
			return mode != Settings.Secure.LOCATION_MODE_OFF;
		}
	}

}
