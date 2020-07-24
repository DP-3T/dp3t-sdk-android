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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.dpppt.android.calibration.MainApplication;
import org.dpppt.android.calibration.R;
import org.dpppt.android.calibration.handshakes.BackendCalibrationReportRepository;
import org.dpppt.android.calibration.util.DialogUtil;
import org.dpppt.android.calibration.util.RequirementsUtil;
import org.dpppt.android.sdk.DP3T;
import org.dpppt.android.sdk.DP3TCalibrationHelper;
import org.dpppt.android.sdk.GaenAvailability;
import org.dpppt.android.sdk.InfectionStatus;
import org.dpppt.android.sdk.TracingStatus;
import org.dpppt.android.sdk.backend.ResponseCallback;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.backend.models.GaenRequest;
import org.dpppt.android.sdk.internal.export.FileUploadRepository;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.ExposeeAuthMethodJson;
import org.dpppt.android.sdk.util.DateUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlsFragment extends Fragment {

	private static final String TAG = ControlsFragment.class.getCanonicalName();

	private static final int REQUEST_CODE_SAVE_DB = 2;
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
		} else if (requestCode == REQUEST_CODE_SAVE_DB && resultCode == Activity.RESULT_OK && data != null) {
			Uri uri = data.getData();
			try {
				OutputStream targetOut = getContext().getContentResolver().openOutputStream(uri);
				DP3TCalibrationHelper.exportDatabase(
						getContext(), targetOut,
						() -> new Handler(Looper.getMainLooper()).post(() -> {
							Toast.makeText(requireContext(), "Export completed!", Toast.LENGTH_LONG).show();
							setExportDbLoadingViewVisible(false);
						}),
						e -> new Handler(Looper.getMainLooper()).post(() -> {
							e.printStackTrace();
							Toast.makeText(requireContext(), "Export failed!", Toast.LENGTH_LONG).show();
						})
				);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
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

		Button batteryButton = view.findViewById(R.id.home_button_battery_optimization);
		batteryButton.setOnClickListener(
				v -> startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
						Uri.parse("package:" + getContext().getPackageName()))));

		Button buttonClearData = view.findViewById(R.id.home_button_clear_data);
		buttonClearData.setOnClickListener(v -> {
			DialogUtil.showConfirmDialog(v.getContext(), R.string.dialog_clear_data_title,
					(dialog, which) -> {
						DP3T.clearData(v.getContext());
						MainApplication.initDP3T(v.getContext());
						v.post(this::updateSdkStatus);
					});
		});

		Button buttonSaveDb = view.findViewById(R.id.home_button_export_db);
		buttonSaveDb.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.setType("application/sqlite");
			intent.putExtra(Intent.EXTRA_TITLE, "dp3t_sample_db.sqlite");
			startActivityForResult(intent, REQUEST_CODE_SAVE_DB);
			setExportDbLoadingViewVisible(true);
		});

		EditText deanonymizationDeviceId = view.findViewById(R.id.deanonymization_device_id);
		Button uploadDB = view.findViewById(R.id.home_button_upload_db);
		uploadDB.setOnClickListener(v -> {
			String deviceId = deanonymizationDeviceId.getText().toString();
			DP3TCalibrationHelper.setCalibrationTestDeviceName(getContext(), deviceId);
			setUploadDbLoadingViewVisible(true);
			new FileUploadRepository()
					.uploadDatabase(requireContext(), AppConfigManager.getInstance(getContext()).getCalibrationTestDeviceName(),
							new Callback<Void>() {
								@Override
								public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
									Toast.makeText(getContext(), "Upload completed!", Toast.LENGTH_LONG).show();
									setUploadDbLoadingViewVisible(false);
								}

								@Override
								public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
									t.printStackTrace();
									Toast.makeText(getContext(), "Upload failed!", Toast.LENGTH_LONG).show();
									setUploadDbLoadingViewVisible(false);
								}
							});
		});

		Button deanonymizationButton = view.findViewById(R.id.deanonymization_key_upload_button);
		deanonymizationButton.setOnClickListener(v -> {
			String deviceId = deanonymizationDeviceId.getText().toString();
			DP3TCalibrationHelper.setCalibrationTestDeviceName(getContext(), deviceId);
			GoogleExposureClient.getInstance(getContext())
					.getTemporaryExposureKeyHistory(getActivity(), 123, temporaryExposureKeys -> {
						GaenRequest exposeeListRequest =
								new GaenRequest(temporaryExposureKeys, DateUtil.getCurrentRollingStartNumber());
						new BackendCalibrationReportRepository(requireContext())
								.addGaenExposee(exposeeListRequest, deviceId, new ResponseCallback<Void>() {
									@Override
									public void onSuccess(Void response) {
										Toast.makeText(getContext(), "Uploaded keys!", Toast.LENGTH_LONG).show();
									}

									@Override
									public void onError(Throwable throwable) {
										Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
									}
								});
					}, e -> {
						Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					});
		});
		if (DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()) != null) {
			deanonymizationDeviceId.setText(DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()));
		}
	}

	private void checkPermissionRequirements() {
		View view = getView();
		Context context = getContext();
		if (view == null || context == null) return;

		Button gaenButton = view.findViewById(R.id.home_button_gaen);
		DP3T.checkGaenAvailability(getContext(), gaenAvailability -> {
			boolean available = gaenAvailability == GaenAvailability.AVAILABLE;
			gaenButton.setEnabled(!available);
			gaenButton.setText(available ? R.string.req_gaen_availabe : R.string.req_gaen_unavailabe);

			gaenButton.setOnClickListener(v -> {
				final String playServicesPackageName = "com.google.android.gms";
				try {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + playServicesPackageName)));
				} catch (android.content.ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW,
							Uri.parse("https://play.google.com/store/apps/details?id=" + playServicesPackageName)));
				}
			});
		});

		boolean bluetoothActivated = RequirementsUtil.isBluetoothEnabled();
		Button bluetoothButton = view.findViewById(R.id.home_button_bluetooth);
		bluetoothButton.setEnabled(!bluetoothActivated);
		bluetoothButton.setText(bluetoothActivated ? R.string.req_bluetooth_active
												   : R.string.req_bluetooth_inactive);

		boolean batteryOptDeactivated = RequirementsUtil.isBatteryOptimizationDeactivated(context);
		Button batteryButton = view.findViewById(R.id.home_button_battery_optimization);
		batteryButton.setEnabled(!batteryOptDeactivated);
		batteryButton.setText(batteryOptDeactivated ? R.string.req_battery_deactivated
													: R.string.req_battery_active);
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
						() -> Toast.makeText(v.getContext(), "EN started", Toast.LENGTH_SHORT).show(),
						(e) -> Toast.makeText(v.getContext(), "EN failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(),
								Toast.LENGTH_SHORT).show(),
						() -> Toast.makeText(v.getContext(), "EN cancelled", Toast.LENGTH_SHORT).show());
			}
			updateSdkStatus();
		});

		Button buttonClearData = view.findViewById(R.id.home_button_clear_data);
		buttonClearData.setEnabled(!isRunning);
		Button buttonSaveDb = view.findViewById(R.id.home_button_export_db);
		buttonSaveDb.setEnabled(!isRunning);
		Button buttonUploadDb = view.findViewById(R.id.home_button_upload_db);
		buttonUploadDb.setEnabled(!isRunning);

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

		Button buttonReportFake = view.findViewById(R.id.home_button_report_fake);
		buttonReportFake.setOnClickListener(
				v -> {
					DP3T.sendFakeInfectedRequest(getContext(), null, null, null);
				});

		EditText deanonymizationDeviceId = view.findViewById(R.id.deanonymization_device_id);
		deanonymizationDeviceId.setText(DP3TCalibrationHelper.getCalibrationTestDeviceName(getContext()));
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
				builder.append("\n").append(error.getErrorString(getContext()));
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

	private void setExportDbLoadingViewVisible(boolean visible) {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.home_loading_view_export_db).setVisibility(visible ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.home_button_export_db).setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}

	private void setUploadDbLoadingViewVisible(boolean visible) {
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.home_loading_view_upload_db).setVisibility(visible ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.home_button_upload_db).setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}

}
