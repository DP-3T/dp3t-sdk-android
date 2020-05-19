package org.dpppt.android.sdk.internal.nearby;

import android.content.Context;

import androidx.annotation.Nullable;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.internal.BroadcastHelper;

public class GaenStateCache {

	private static GaenAvailability gaenAvailability = null;
	private static Boolean gaenEnabled = null;

	public static GaenAvailability getGaenAvailability() {
		return gaenAvailability;
	}

	public static void setGaenAvailability(GaenAvailability gaenAvailability, Context context) {
		if (GaenStateCache.gaenAvailability != gaenAvailability) {
			GaenStateCache.gaenAvailability = gaenAvailability;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

	@Nullable
	public static Boolean isGaenEnabled() {
		return gaenEnabled;
	}

	public static void setGaenEnabled(boolean gaenEnabled, Context context) {
		if (!Boolean.valueOf(gaenEnabled).equals(GaenStateCache.gaenEnabled)) {
			GaenStateCache.gaenEnabled = gaenEnabled;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
