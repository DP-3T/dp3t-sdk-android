/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.calibration.parameters;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.dpppt.android.calibration.R;
import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BluetoothAdvertiseMode;
import org.dpppt.android.sdk.internal.BluetoothScanMode;
import org.dpppt.android.sdk.internal.BluetoothTxPowerLevel;

public class ParametersFragment extends Fragment {

	private static final int MIN_INTERVAL_SCANNING_SECONDS = 30;
	private static final int MAX_INTERVAL_SCANNING_SECONDS = 900;
	private static final int MIN_DURATION_SCANNING_SECONDS = 10;
	private Spinner spinnerScanMode;
	private Spinner spinnerUseScanResponse;
	private Spinner spinnerAdvertisingMode;
	private Spinner spinnerPowerLevel;
	private SeekBar seekBarScanInterval;
	private SeekBar seekBarScanDuration;
	private EditText inputScanInterval;
	private EditText inputScanDuration;

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

		seekBarScanInterval = view.findViewById(R.id.parameter_seekbar_scan_interval);
		inputScanInterval = view.findViewById(R.id.parameter_input_scan_interval);
		seekBarScanDuration = view.findViewById(R.id.parameter_seekbar_scan_duration);
		inputScanDuration = view.findViewById(R.id.parameter_input_scan_duration);

		spinnerScanMode = view.findViewById(R.id.parameter_spinner_scan_mode);
		ArrayAdapter<BluetoothScanMode> scanModeAdapter =
				new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, BluetoothScanMode.values());
		spinnerScanMode.setAdapter(scanModeAdapter);
		spinnerScanMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setScanMode(BluetoothScanMode.values()[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
		});

		seekBarScanInterval.setMax(MAX_INTERVAL_SCANNING_SECONDS - MIN_INTERVAL_SCANNING_SECONDS);
		seekBarScanInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int intervalDuration = progress + MIN_INTERVAL_SCANNING_SECONDS;
				inputScanInterval.setText(String.valueOf(intervalDuration));
				int newMaxProgress = intervalDuration - 1 - MIN_DURATION_SCANNING_SECONDS;
				adjustNewDurationMaximum(newMaxProgress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				setScanInterval(seekBar.getProgress() + MIN_INTERVAL_SCANNING_SECONDS);
			}
		});
		inputScanInterval.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				String input = inputScanInterval.getText().toString();
				if (input.length() == 0) return true;
				try {
					int inputIntervalSeconds = Integer.parseInt(input);
					inputIntervalSeconds =
							Math.min(MAX_INTERVAL_SCANNING_SECONDS, Math.max(MIN_INTERVAL_SCANNING_SECONDS, inputIntervalSeconds));
					inputScanInterval.setText(String.valueOf(inputIntervalSeconds));
					seekBarScanInterval.setProgress(inputIntervalSeconds - MIN_INTERVAL_SCANNING_SECONDS);
					setScanInterval(inputIntervalSeconds);
					hideKeyboard(v);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		});

		seekBarScanDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int scanDuration = progress + MIN_DURATION_SCANNING_SECONDS;
				inputScanDuration.setText(String.valueOf(scanDuration));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				setScanDuration(seekBar.getProgress() + MIN_DURATION_SCANNING_SECONDS);
			}
		});
		inputScanDuration.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				String input = inputScanDuration.getText().toString();
				if (input.length() == 0) return true;
				try {
					int inputDurationSeconds = Integer.parseInt(input);
					inputDurationSeconds =
							Math.min(getScanInterval() - 1, Math.max(MIN_DURATION_SCANNING_SECONDS, inputDurationSeconds));
					inputScanDuration.setText(String.valueOf(inputDurationSeconds));
					seekBarScanDuration.setProgress(inputDurationSeconds - MIN_DURATION_SCANNING_SECONDS);
					setScanDuration(inputDurationSeconds);
					hideKeyboard(v);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		});

		spinnerUseScanResponse = view.findViewById(R.id.parameter_spinner_use_scan_response);
		ArrayAdapter<Boolean> useScanResponseAdapter =
				new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new Boolean[] { false, true });
		spinnerUseScanResponse.setAdapter(useScanResponseAdapter);
		spinnerUseScanResponse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setUseScanResponse(position == 1);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
		});

		spinnerAdvertisingMode = view.findViewById(R.id.parameter_spinner_advertising_mode);
		ArrayAdapter<BluetoothAdvertiseMode> advertisingModeAdapter =
				new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, BluetoothAdvertiseMode.values());
		spinnerAdvertisingMode.setAdapter(advertisingModeAdapter);
		spinnerAdvertisingMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setAdvertisingMode(BluetoothAdvertiseMode.values()[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
		});

		spinnerPowerLevel = view.findViewById(R.id.parameter_spinner_power_level);
		ArrayAdapter<BluetoothTxPowerLevel> powerLevelAdapter =
				new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, BluetoothTxPowerLevel.values());
		spinnerPowerLevel.setAdapter(powerLevelAdapter);
		spinnerPowerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setAdvertPowerLevel(BluetoothTxPowerLevel.values()[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
		});

		TextView version_info = view.findViewById(R.id.version_info);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
		version_info.setText(
				BuildConfig.VERSION_NAME + " / " + sdf.format(BuildConfig.BUILD_TIME) + " / " + BuildConfig.FLAVOR + " / " +
						BuildConfig.BUILD_TYPE);
	}

	private void adjustNewDurationMaximum(int durationProgressMaximum) {
		int currentDurationProgress = seekBarScanDuration.getProgress();
		seekBarScanDuration.setMax(durationProgressMaximum);
		if (currentDurationProgress > durationProgressMaximum) {
			setScanDuration(durationProgressMaximum + MIN_DURATION_SCANNING_SECONDS);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		AppConfigManager appConfigManager = AppConfigManager.getInstance(getContext());

		BluetoothScanMode scanMode = appConfigManager.getBluetoothScanMode();
		spinnerScanMode.setSelection(scanMode.ordinal());

		int interval = (int) (appConfigManager.getScanInterval() / 1000);
		seekBarScanInterval.setProgress(interval - MIN_INTERVAL_SCANNING_SECONDS);
		int duration = (int) (appConfigManager.getScanDuration() / 1000);
		seekBarScanDuration.setProgress(duration - MIN_DURATION_SCANNING_SECONDS);

		boolean useScanResponse = appConfigManager.isScanResponseEnabled();
		spinnerUseScanResponse.setSelection(useScanResponse ? 1 : 0);

		BluetoothAdvertiseMode selectedMode = appConfigManager.getBluetoothAdvertiseMode();
		spinnerAdvertisingMode.setSelection(selectedMode.ordinal());

		BluetoothTxPowerLevel selectedLevel = appConfigManager.getBluetoothTxPowerLevel();
		spinnerPowerLevel.setSelection(selectedLevel.ordinal());
	}

	private void setScanMode(BluetoothScanMode mode) {
		AppConfigManager.getInstance(getContext()).setBluetoothScanMode(mode);
	}

	private int getScanInterval() {
		return seekBarScanInterval.getProgress() + MIN_INTERVAL_SCANNING_SECONDS;
	}

	private void setScanInterval(int interval) {
		AppConfigManager.getInstance(getContext()).setScanInterval(interval * 1000);
	}

	private void setScanDuration(int duration) {
		AppConfigManager.getInstance(getContext()).setScanDuration(duration * 1000);
	}

	private void setAdvertPowerLevel(BluetoothTxPowerLevel powerLevel) {
		AppConfigManager.getInstance(getContext()).setBluetoothPowerLevel(powerLevel);
	}

	private void setAdvertisingMode(BluetoothAdvertiseMode mode) {
		AppConfigManager.getInstance(getContext()).setBluetoothAdvertiseMode(mode);
	}

	private void setUseScanResponse(boolean useScanResponse) {
		AppConfigManager.getInstance(getContext()).setUseScanResponse(useScanResponse);
	}

	private void hideKeyboard(View view) {
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

}
