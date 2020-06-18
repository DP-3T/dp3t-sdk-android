package org.dpppt.android.sdk.internal.util;

import android.content.Context;
import android.content.pm.PackageManager;

public class PackageManagerUtil {

	public static boolean isPackageInstalled(String packageName, Context context) {
		try {
			context.getPackageManager().getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

}
