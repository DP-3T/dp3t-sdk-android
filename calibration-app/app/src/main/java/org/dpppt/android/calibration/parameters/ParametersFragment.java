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
import org.dpppt.android.sdk.internal.AppConfigManager;

public class ParametersFragment extends Fragment {

	AppConfigManager appConfigManager;

	AppCompatSeekBar attenuationBucket1Seeekbar;
	EditText attenuationBucket1Text;
	AppCompatSeekBar attenuationBucket2Seeekbar;
	EditText attenuationBucket2Text;

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

		appConfigManager = AppConfigManager.getInstance(getContext());

		attenuationBucket1Seeekbar = view.findViewById(R.id.parameter_seekbar_attenuation_bucket1);
		attenuationBucket1Text = view.findViewById(R.id.parameter_seekbar_attenuation_bucket1_value);

		attenuationBucket1Seeekbar.setMax(254);
		attenuationBucket1Seeekbar.setProgress(appConfigManager.getAttenuationThresholdLow());
		attenuationBucket1Seeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				setBucket1Value(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});
		attenuationBucket1Text.setText(Integer.toString(appConfigManager.getAttenuationThresholdLow()));
		attenuationBucket1Text.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				String input = attenuationBucket1Text.getText().toString();
				if (input.length() == 0) return true;
				try {
					int value = Integer.parseInt(input);
					attenuationBucket1Seeekbar.setProgress(value);
					hideKeyboard(v);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		});

		attenuationBucket2Seeekbar = view.findViewById(R.id.parameter_seekbar_attenuation_bucket2);
		attenuationBucket2Text = view.findViewById(R.id.parameter_seekbar_attenuation_bucket2_value);

		attenuationBucket2Seeekbar.setMax(255);
		attenuationBucket2Seeekbar.setProgress(appConfigManager.getAttenuationThresholdMedium());
		attenuationBucket2Seeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int min = 1;
				if (progress < min) progress = min;
				setBucket2Value(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});
		attenuationBucket2Text.setText(Integer.toString(appConfigManager.getAttenuationThresholdMedium()));
		attenuationBucket2Text.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				String input = attenuationBucket2Text.getText().toString();
				if (input.length() == 0) return true;
				try {
					int value = Integer.parseInt(input);
					attenuationBucket2Seeekbar.setProgress(value);
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

	private void setBucket1Value(int thresholdLow) {
		int thresholdMedium = Math.max(attenuationBucket2Seeekbar.getProgress(), thresholdLow + 1);
		appConfigManager.setAttenuationThresholds(thresholdLow, thresholdMedium);
		attenuationBucket1Text.setText(Integer.toString(thresholdLow));
		attenuationBucket2Seeekbar.setProgress(thresholdMedium);
	}

	private void setBucket2Value(int thresholdMedium) {
		int thresholdLow = Math.min(attenuationBucket1Seeekbar.getProgress(), thresholdMedium - 1);
		appConfigManager.setAttenuationThresholds(thresholdLow, thresholdMedium);
		attenuationBucket2Text.setText(Integer.toString(thresholdMedium));
		attenuationBucket1Seeekbar.setProgress(thresholdLow);
	}


	private void hideKeyboard(View view) {
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

}
