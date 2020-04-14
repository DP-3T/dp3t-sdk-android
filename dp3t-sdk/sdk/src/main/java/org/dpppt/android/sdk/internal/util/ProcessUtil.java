/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.sdk.internal.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ProcessUtil {

	public static boolean isMainProcess(Context context) {
		return context.getPackageName().equals(getProcessName(context));
	}

	private static String getProcessName(Context context) {
		int mypid = android.os.Process.myPid();
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo info : infos) {
			if (info.pid == mypid) {
				return info.processName;
			}
		}
		// may never return null
		return null;
	}

}
