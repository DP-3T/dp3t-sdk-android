/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration.handshakes;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureWindow;
import com.google.android.gms.nearby.exposurenotification.ScanInstance;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import org.dpppt.android.calibration.R;
import org.dpppt.android.sdk.DP3TCalibrationHelper;
import org.dpppt.android.sdk.internal.export.FileUploadRepository;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.internal.util.Json;
import org.dpppt.android.sdk.models.DayDate;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HandshakesFragment extends Fragment {

	private static final String TAG = "HandshakeFragment";

	LinearLayout layout;

	public static HandshakesFragment newInstance() {
		return new HandshakesFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_handshakes, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		layout = view.findViewById(R.id.handshake_list);

		load();
	}

	private void load() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		layout.removeAllViews();
		new Thread(() -> {
			try {
				GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
				long batchReleaseTime = new DayDate().addDays(1).getStartOfDayTimestamp();
				BackendUserBucketRepository backendBucketRepository = new BackendUserBucketRepository(context);
				ResponseBody responseBody = backendBucketRepository.getGaenExposees(batchReleaseTime);

				HashMap<String, Experiment> experiments = new HashMap<>();

				ZipInputStream zis = new ZipInputStream(responseBody.byteStream());
				ZipEntry zipEntry;
				while ((zipEntry = zis.getNextEntry()) != null) {
					String name = zipEntry.getName();
					Experiment experiment;
					String deviceName = "unknown";
					Matcher matcher = Pattern.compile("key_export_experiment_([a-zA-Z0-9]+)_(.+)").matcher(name);
					if (matcher.find()) {
						String experimentName = matcher.group(1);
						deviceName = matcher.group(2);
						experiment = experiments.get(experimentName);
						if (experiment == null) {
							experiment = new Experiment(experimentName);
							experiments.put(experimentName, experiment);
						}
					} else {
						deviceName = name.substring(11);
						experiment = new Experiment("SingleDevice: " + deviceName);
						experiments.put(experiment.getName(), experiment);
					}

					File file = new File(context.getCacheDir(), batchReleaseTime + "_" + name);
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
					byte[] bytesIn = new byte[1024];
					int read = 0;
					while ((read = zis.read(bytesIn)) != -1) {
						bos.write(bytesIn, 0, read);
					}
					bos.close();
					zis.closeEntry();

					experiment.devices.add(new Experiment.Device(deviceName, file));
				}

				getView().post(() -> {
					for (Experiment experiment : experiments.values()) {
						View view = getLayoutInflater().inflate(R.layout.item_handshake, layout, false);
						((TextView) view.findViewById(R.id.device_name)).setText("Experiment: " + experiment.name);
						TextView textView = view.findViewById(R.id.device_info);
						textView.setText("Devices: " +
								experiment.getDevices().stream().map((device -> device.getName())).reduce((a, b) -> a + ", " + b)
										.get());
						layout.addView(view);
						view.setOnClickListener(v -> {
							v.setOnClickListener(null);
							textView.setText("Loading...");
							new Thread(() -> {
								HashMap<String, List<ExposureWindow>> result = new HashMap<>();
								List<ExposureWindow> oldExposureWindows = new ArrayList<>();
								for (Experiment.Device device : experiment.devices) {
									try {
										ArrayList<File> fileList = new ArrayList<>();
										fileList.add(device.file);
										googleExposureClient.provideDiagnosisKeys(fileList, ExposureNotificationClient.TOKEN_A);
										Thread.sleep(2000);
										List<ExposureWindow> newExposureWindows = googleExposureClient.getExposureWindows();
										Iterator<ExposureWindow> iterator = newExposureWindows.iterator();
										while (iterator.hasNext()) {
											if (oldExposureWindows.contains(iterator.next())) {
												iterator.remove();
											}
										}
										oldExposureWindows.addAll(newExposureWindows);
										result.put(device.getName(), newExposureWindows);
									} catch (Exception e) {
										e.printStackTrace();
										view.post(() -> {
											textView.setText("Exception: " + e.getLocalizedMessage());
										});
									}
								}
								File file = new File(context.getFilesDir(),
										"result_experiment_" + experiment.name + "_" + new DayDate().formatAsString() +
												"_device_" +
												DP3TCalibrationHelper.getInstance(context).getCalibrationTestDeviceName() +
												".json");
								try {
									new BufferedWriter(new FileWriter(file)).append(GSON.toJson(result)).close();
								} catch (IOException e) {
									e.printStackTrace();
								}
								new FileUploadRepository().uploadFile(file, new Callback<Void>() {
									@Override
									public void onResponse(Call<Void> call, Response<Void> response) {
										view.post(() -> {
											textView.setText(GSON.toJson(result));
										});
									}

									@Override
									public void onFailure(Call<Void> call, Throwable t) {
										view.post(() -> {
											textView.setText("Upload failed: " + t.getMessage());
										});
									}
								});
							}).start();
						});
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private static JsonSerializer<ExposureWindow> exposureWindowJsonSerializer = (src, typeOfSrc, context) -> {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("dateMillisSinceEpoch", src.getDateMillisSinceEpoch());
		jsonObject.addProperty("reportType", src.getReportType());
		jsonObject.add("scanInstances", context.serialize(src.getScanInstances()));

		return jsonObject;
	};

	private static JsonSerializer<ScanInstance> scanInstanceJsonSerializer = (src, typeOfSrc, context) -> {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("minAttenuationDb", src.getMinAttenuationDb());
		jsonObject.addProperty("typicalAttenuationDb", src.getTypicalAttenuationDb());
		jsonObject.addProperty("secondsSinceLastScan", src.getSecondsSinceLastScan());

		return jsonObject;
	};
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ExposureWindow.class, exposureWindowJsonSerializer)
			.registerTypeAdapter(ScanInstance.class, scanInstanceJsonSerializer)
			.create();
}
