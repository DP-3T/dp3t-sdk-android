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
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.dpppt.android.calibration.R;
import org.dpppt.android.calibration.handshakes.BackendCalibrationReportRepository;
import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.DP3TCalibrationHelper;
import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.util.DateUtil;

public class ParametersFragment extends Fragment {

	private static final int RESOLUTION_REQUEST_CODE = 123;

	AppConfigManager appConfigManager;
	EditText experimentIdEditText;
	EditText deviceIdEditText;

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

		experimentIdEditText = view.findViewById(R.id.experiment_id);
		deviceIdEditText = view.findViewById(R.id.experiment_device_id);

		Button deanonymizationButton = view.findViewById(R.id.deanonymization_key_upload_button);
		deanonymizationButton.setOnClickListener(v -> uploadKeys());

		experimentIdEditText.setText(DP3TCalibrationHelper.getInstance(getContext()).getExperimentName());
		deviceIdEditText.setText(DP3TCalibrationHelper.getInstance(getContext()).getCalibrationTestDeviceName());

		TextView version_info = view.findViewById(R.id.version_info);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		version_info.setText(
				BuildConfig.VERSION_NAME + " / " + sdf.format(BuildConfig.BUILD_TIME) + " / " + BuildConfig.FLAVOR + " / " +
						BuildConfig.BUILD_TYPE);
	}

	private void uploadKeys() {
		String experimentId = experimentIdEditText.getText().toString();
		String deviceId = deviceIdEditText.getText().toString();
		DP3TCalibrationHelper.getInstance(getContext()).setExperimentName(experimentId);
		DP3TCalibrationHelper.getInstance(getContext()).setCalibrationTestDeviceName(deviceId);
		String name = "experiment_" + experimentId + "_" + deviceId;
		GoogleExposureClient.getInstance(getContext())
				.getTemporaryExposureKeyHistory(getActivity(), RESOLUTION_REQUEST_CODE, temporaryExposureKeys -> {
					ProgressDialog progressDialog = new ProgressDialog(getContext());
					progressDialog.show();
					GaenRequest exposeeListRequest =
							new GaenRequest(temporaryExposureKeys, DateUtil.getCurrentRollingStartNumber());
					new BackendCalibrationReportRepository(requireContext())
							.addGaenExposee(exposeeListRequest, name,
									new ResponseCallback<Void>() {
										@Override
										public void onSuccess(Void response) {
											progressDialog.dismiss();
											Toast.makeText(getContext(), "Uploaded keys!", Toast.LENGTH_LONG).show();
										}

										@Override
										public void onError(Throwable throwable) {
											progressDialog.dismiss();
											Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_LONG)
													.show();
										}
									});
				}, e -> {
					Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == RESOLUTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			uploadKeys();
		}
	}

	private void hideKeyboard(View view) {
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

}
