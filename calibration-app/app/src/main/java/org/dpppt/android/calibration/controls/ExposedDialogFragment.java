/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.calibration.controls;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.dpppt.android.calibration.R;
import org.dpppt.android.calibration.util.DatePickerFragmentDialog;

public class ExposedDialogFragment extends DialogFragment {

	public static final String RESULT_EXTRA_DATE_MILLIS = "result_extra_date_millis";
	public static final String RESULT_EXTRA_AUTH_CODE_INPUT_BASE64 = "result_extra_auth_code_input_base64";

	private static final String ARG_MIN_DATE = "arg_min_date";
	private static final String ARG_CODE_REGEX = "arg_code_regex";
	private static final int REQUEST_CODE_DATE_PICKER = 1;

	private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();

	private long minDate;
	private String authCodeRegex;

	private long selectedDate = -1;

	private EditText codeInputView;
	private TextView errorView;
	private EditText dateView;

	public static ExposedDialogFragment newInstance(long minDate, String authCodeValidityRegex) {
		Bundle args = new Bundle();
		args.putLong(ARG_MIN_DATE, minDate);
		args.putString(ARG_CODE_REGEX, authCodeValidityRegex);
		ExposedDialogFragment fragment = new ExposedDialogFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		minDate = getArguments().getLong(ARG_MIN_DATE);
		authCodeRegex = getArguments().getString(ARG_CODE_REGEX);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_fragment_exposed, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		dateView = view.findViewById(R.id.input_dialog_date);
		dateView.setHint(DATE_FORMAT.format(new Date()));
		dateView.setOnClickListener(v -> {
			DialogFragment newFragment = DatePickerFragmentDialog
					.newInstance(minDate, selectedDate > 0 ? selectedDate : Calendar.getInstance().getTimeInMillis());
			newFragment.setTargetFragment(this, REQUEST_CODE_DATE_PICKER);
			newFragment.show(getParentFragmentManager(), DatePickerFragmentDialog.class.getCanonicalName());
		});
		codeInputView = view.findViewById(R.id.input_dialog_input);
		errorView = view.findViewById(R.id.input_dialog_error_text);

		Button positiveButton = view.findViewById(R.id.input_dialog_positive_button);
		positiveButton.setOnClickListener(v -> {
			checkAndDismissDialog(selectedDate, codeInputView.getText().toString());
		});
		Button negativeButton = view.findViewById(R.id.input_dialog_negative_button);
		negativeButton.setOnClickListener(v -> {
			dismissDialog();
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQUEST_CODE_DATE_PICKER && resultCode == Activity.RESULT_OK) {
			selectedDate = data.getLongExtra(DatePickerFragmentDialog.RESULT_EXTRA_DATE_MILLIS, -1);
			dateView.setText(DATE_FORMAT.format(new Date(selectedDate)));
		}
	}

	private void checkAndDismissDialog(long selectedDate, String codeInput) {
		boolean dateValid = selectedDate > minDate && selectedDate <= Calendar.getInstance().getTimeInMillis();
		if (!dateValid) {
			errorView.setText(R.string.dialog_input_date_error);
			errorView.setVisibility(View.VISIBLE);
			return;
		}
		boolean codeValid = authCodeRegex == null || codeInput.matches(authCodeRegex);
		if (!codeValid) {
			errorView.setText(R.string.dialog_input_code_error);
			errorView.setVisibility(View.VISIBLE);
			return;
		}

		Intent result = new Intent();
		result.putExtra(RESULT_EXTRA_DATE_MILLIS, selectedDate);
		String inputBase64 = new String(Base64.encode(codeInput.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP),
				StandardCharsets.UTF_8);
		result.putExtra(RESULT_EXTRA_AUTH_CODE_INPUT_BASE64, inputBase64);
		getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, result);
		dismiss();
	}

	private void dismissDialog() {
		getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
		dismiss();
	}

}
