package org.dpppt.android.sdk.internal.nearby;

import android.content.Context;

import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.internal.BroadcastHelper;

public class GaenStateCache {

	private static GaenAvailability gaenAvailability;
	private static boolean gaenEnabled = false;

	public static GaenAvailability getGaenAvailability() {
		return gaenAvailability;
	}

	public static void setGaenAvailability(GaenAvailability gaenAvailability, Context context) {
		if (GaenStateCache.gaenAvailability != gaenAvailability) {
			GaenStateCache.gaenAvailability = gaenAvailability;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

	public static boolean isGaenEnabled() {
		return gaenEnabled;
	}

	public static void setGaenEnabled(boolean gaenEnabled, Context context) {
		if (GaenStateCache.gaenEnabled != gaenEnabled) {
			GaenStateCache.gaenEnabled = gaenEnabled;
			BroadcastHelper.sendUpdateAndErrorBroadcast(context);
		}
	}

}
