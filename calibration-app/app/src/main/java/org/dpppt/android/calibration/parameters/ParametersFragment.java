/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration.parameters;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.dpppt.android.calibration.R;
import org.dpppt.android.sdk.BuildConfig;

public class ParametersFragment extends Fragment {

	public static ParametersFragment newInstance() {
		return new ParametersFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_parameters, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		AppCompatSeekBar attenuationBucket1Seeekbar = view.findViewById(R.id.parameter_seekbar_attenuation_bucket1);
		EditText attenuationBucket1Text = view.findViewById(R.id.parameter_seekbar_attenuation_bucket1_value);

		attenuationBucket1Seeekbar.setMax(255);
		attenuationBucket1Seeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				attenuationBucket1Text.setText(Integer.toString(progress));
				//TODO set value
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//TODO set value
			}
		});
		attenuationBucket1Text.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				String input = attenuationBucket1Text.getText().toString();
				if (input.length() == 0) return true;
				try {
					int value = Integer.parseInt(input);
					attenuationBucket1Text.setText(String.valueOf(value));
					attenuationBucket1Seeekbar.setProgress(value);
					//TODO set value
					hideKeyboard(v);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		});

		TextView version_info = view.findViewById(R.id.version_info);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		version_info.setText(
				BuildConfig.VERSION_NAME + " / " + sdf.format(BuildConfig.BUILD_TIME) + " / " + BuildConfig.FLAVOR + " / " +
						BuildConfig.BUILD_TYPE);
	}

	private void hideKeyboard(View view) {
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

}
