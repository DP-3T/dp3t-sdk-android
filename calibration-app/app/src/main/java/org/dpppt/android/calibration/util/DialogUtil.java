/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.calibration.util;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import org.dpppt.android.calibration.R;

public class DialogUtil {

	public static void showConfirmDialog(Context context, @StringRes int title,
			DialogInterface.OnClickListener positiveClickListener) {
		new AlertDialog.Builder(context)
				.setTitle(title)
				.setMessage(R.string.dialog_confirm_message)
				.setPositiveButton(R.string.dialog_confirm_positive_button, (dialog, which) -> {
					dialog.dismiss();
					positiveClickListener.onClick(dialog, which);
				})
				.setNegativeButton(R.string.dialog_confirm_negative_button, (dialog, which) -> dialog.dismiss())
				.show();
	}

	public static void showMessageDialog(Context context, String title, String msg) {
		new AlertDialog.Builder(context)
				.setTitle(title)
				.setMessage(msg)
				.setPositiveButton(R.string.dialog_button_ok, (dialog, which) -> dialog.dismiss())
				.show();
	}

}
