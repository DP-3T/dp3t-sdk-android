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
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CancellationException;

import org.dpppt.android.calibration.MainApplication;
import org.dpppt.android.calibration.R;
import org.dpppt.android.calibration.util.DialogUtil;
import org.dpppt.android.calibration.util.RequirementsUtil;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.DP3TCalibrationHelper;
import org.dpppt.android.sdk.InfectionStatus;
import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.models.ExposeeAuthMethodJson;

public class ControlsFragment extends Fragment {

	private static final String TAG = ControlsFragment.class.getCanonicalName();

	private static final int REQUEST_CODE_REPORT_EXPOSED = 3;
	private static final int REQUEST_CODE_ENABLE_BLE = 4;

	private static final DateFormat DATE_FORMAT_SYNC = SimpleDateFormat.getDateTimeInstance();

	private static final String REGEX_VALIDITY_AUTH_CODE = "\\w+";
	private static final int EXPOSED_MIN_DATE_DIFF = -21;

	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				checkPermissionRequirements();
				updateSdkStatus();
			}
		}
	};

	private BroadcastReceiver sdkReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateSdkStatus();
		}
	};

	public static ControlsFragment newInstance() {
		return new ControlsFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupUi(view);
	}

	@Override
	public void onResume() {
		super.onResume();
		getContext().registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		getContext().registerReceiver(sdkReceiver, DP3T.getUpdateIntentFilter());
		checkPermissionRequirements();
		updateSdkStatus();
	}

	@Override
	public void onPause() {
		super.onPause();
		getContext().unregisterReceiver(bluetoothReceiver);
		getContext().unregisterReceiver(sdkReceiver);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == REQUEST_CODE_REPORT_EXPOSED) {
			if (resultCode == Activity.RESULT_OK) {
				long onsetDate = data.getLongExtra(ExposedDialogFragment.RESULT_EXTRA_DATE_MILLIS, -1);
				String authCodeBase64 = data.getStringExtra(ExposedDialogFragment.RESULT_EXTRA_AUTH_CODE_INPUT_BASE64);
				sendInfectedUpdate(new Date(onsetDate), authCodeBase64);
			}
		} else if (requestCode == REQUEST_CODE_ENABLE_BLE) {
			// handled by bluetoothReceiver
		}
	}

	private void setupUi(View view) {
		Button bluetoothButton = view.findViewById(R.id.home_button_bluetooth);
		bluetoothButton.setOnClickListener(v -> {
			Intent bleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(bleIntent, REQUEST_CODE_ENABLE_BLE);
		});

		Button refreshButton = view.findViewById(R.id.home_button_sync);
		refreshButton.setOnClickListener(v -> resyncSdk());

		Button buttonClearData = view.findViewById(R.id.home_button_clear_data);
		buttonClearData.setOnClickListener(v -> {
			DialogUtil.showConfirmDialog(v.getContext(), R.string.dialog_clear_data_title,
					(dialog, which) -> {
						DP3T.clearData(v.getContext());
						MainApplication.initDP3T(v.getContext());
						v.post(this::updateSdkStatus);
					});
		});

		EditText deanonymizationDeviceId = view.findViewById(R.id.deanonymization_device_id);
		Switch deanonymizationSwitch = view.findViewById(R.id.deanonymization_switch);
		if (DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()) != null) {
			deanonymizationSwitch.setChecked(true);
			deanonymizationDeviceId.setText(DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()));
		}
		deanonymizationSwitch.setOnCheckedChangeListener((compoundButton, enabled) -> {
			if (enabled) {
				setDeviceId(deanonymizationDeviceId.getText().toString());
			} else {
				DP3TCalibrationHelper.disableCalibrationTestDeviceName(getContext());
			}
		});
		deanonymizationDeviceId.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (deanonymizationSwitch.isChecked()) {
					setDeviceId(editable.toString());
				}
			}
		});
	}

	private void setDeviceId(String deviceId) {
		if (deviceId.length() > 4) {
			deviceId = deviceId.substring(0, 4);
		} else {
			while (deviceId.length() < 4) {
				deviceId = deviceId + " ";
			}
		}
		DP3TCalibrationHelper.setCalibrationTestDeviceName(getContext(), deviceId);
	}

	private void checkPermissionRequirements() {
		View view = getView();
		Context context = getContext();
		if (view == null || context == null) return;

		boolean bluetoothActivated = RequirementsUtil.isBluetoothEnabled();
		Button bluetoothButton = view.findViewById(R.id.home_button_bluetooth);
		bluetoothButton.setEnabled(!bluetoothActivated);
		bluetoothButton.setText(bluetoothActivated ? R.string.req_bluetooth_active
												   : R.string.req_bluetooth_inactive);
	}

	private void resyncSdk() {
		new Thread(() -> {
			DP3T.sync(getContext());
			new Handler(getContext().getMainLooper()).post(this::updateSdkStatus);
		}).start();
	}

	private void updateSdkStatus() {
		View view = getView();
		Context context = getContext();
		if (context == null || view == null) return;

		TracingStatus status = DP3T.getStatus(context);

		TextView statusText = view.findViewById(R.id.home_status_text);
		statusText.setText(formatStatusString(status));

		Button buttonStartStopTracking = view.findViewById(R.id.home_button_start_stop_tracking);
		boolean isRunning = status.isTracingEnabled();
		buttonStartStopTracking.setSelected(isRunning);
		buttonStartStopTracking.setText(getString(isRunning ? R.string.button_tracking_stop
															: R.string.button_tracking_start));
		buttonStartStopTracking.setOnClickListener(v -> {
			if (isRunning) {
				DP3T.stop(v.getContext());
			} else {
				DP3T.start(getActivity(),
						() -> {
							Toast.makeText(v.getContext(), "EN started successfully", Toast.LENGTH_SHORT).show();
						},
						(e) -> {
							if (!(e instanceof CancellationException)) {
								Toast.makeText(v.getContext(),
										"EN failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
										Toast.LENGTH_SHORT).show();
							}
						});
			}
			updateSdkStatus();
		});

		Button buttonClearData = view.findViewById(R.id.home_button_clear_data);
		buttonClearData.setEnabled(!isRunning);

		Button buttonReportInfected = view.findViewById(R.id.home_button_report_infected);
		buttonReportInfected.setEnabled(status.getInfectionStatus() != InfectionStatus.INFECTED);
		buttonReportInfected.setText(R.string.button_report_infected);
		buttonReportInfected.setOnClickListener(
				v -> {
					Calendar minCal = Calendar.getInstance();
					minCal.add(Calendar.DAY_OF_YEAR, EXPOSED_MIN_DATE_DIFF);
					DialogFragment exposedDialog =
							ExposedDialogFragment.newInstance(minCal.getTimeInMillis(), REGEX_VALIDITY_AUTH_CODE);
					exposedDialog.setTargetFragment(this, REQUEST_CODE_REPORT_EXPOSED);
					exposedDialog.show(getParentFragmentManager(), ExposedDialogFragment.class.getCanonicalName());
				});

		EditText deanonymizationDeviceId = view.findViewById(R.id.deanonymization_device_id);
		Switch deanonymizationSwitch = view.findViewById(R.id.deanonymization_switch);
		if (DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()) != null) {
			deanonymizationSwitch.setChecked(true);
			deanonymizationDeviceId.setText(DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()));
		} else {
			deanonymizationSwitch.setChecked(false);
			deanonymizationDeviceId.setText("0000");
		}
	}

	private SpannableString formatStatusString(TracingStatus status) {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		boolean isTracking = status.isTracingEnabled();
		builder.append(getString(isTracking ? R.string.status_tracking_active : R.string.status_tracking_inactive)).append("\n")
				.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length() - 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		builder.append(getString(R.string.status_advertising, status.isTracingEnabled())).append("\n")
				.append(getString(R.string.status_receiving, status.isTracingEnabled())).append("\n");

		long lastSyncDateUTC = status.getLastSyncDate();
		String lastSyncDateString =
				lastSyncDateUTC > 0 ? DATE_FORMAT_SYNC.format(new Date(lastSyncDateUTC)) : "n/a";
		builder.append(getString(R.string.status_last_synced, lastSyncDateString)).append("\n")
				.append(getString(R.string.status_self_infected, status.getInfectionStatus() == InfectionStatus.INFECTED))
				.append("\n")
				.append(getString(R.string.status_been_exposed, status.getInfectionStatus() == InfectionStatus.EXPOSED))
				.append("\n");

		Collection<TracingStatus.ErrorState> errors = status.getErrors();
		if (errors != null && errors.size() > 0) {
			int start = builder.length();
			builder.append("\n");
			for (TracingStatus.ErrorState error : errors) {
				builder.append("\n").append(error.toString());
			}
			builder.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.red, null)),
					start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return new SpannableString(builder);
	}

	private void sendInfectedUpdate(Date onsetDate, String codeInputBase64) {
		setExposeLoadingViewVisible(true);

		DP3T.sendIAmInfected(getActivity(), onsetDate, new ExposeeAuthMethodJson(codeInputBase64), new ResponseCallback<Void>() {
			@Override
			public void onSuccess(Void response) {
				DialogUtil.showMessageDialog(getContext(), getString(R.string.dialog_title_success),
						getString(R.string.dialog_message_request_success));
				setExposeLoadingViewVisible(false);
				updateSdkStatus();
			}

			@Override
			public void onError(Throwable throwable) {
				DialogUtil.showMessageDialog(getContext(), getString(R.string.dialog_title_error),
						throwable.getLocalizedMessage());
				Log.e(TAG, throwable.getMessage(), throwable);
				setExposeLoadingViewVisible(false);
			}
		});
	}

	private void setExposeLoadingViewVisible(boolean visible) {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.home_loading_view_exposed).setVisibility(visible ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.home_button_report_infected).setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}

}
