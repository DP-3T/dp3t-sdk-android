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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dpppt.android.calibration.R;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.DayDate;

import okhttp3.ResponseBody;

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
				long currentTime = System.currentTimeMillis();
				long batchReleaseTime = new DayDate().addDays(1).getStartOfDayTimestamp();
				BackendUserBucketRepository backendBucketRepository = new BackendUserBucketRepository(context);
				ResponseBody result = backendBucketRepository.getGaenExposees(batchReleaseTime);

				ZipInputStream zis = new ZipInputStream(result.byteStream());
				ZipEntry zipEntry;
				while ((zipEntry = zis.getNextEntry()) != null) {
					File file = new File(context.getCacheDir(), batchReleaseTime + "_" + zipEntry.getName());
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
					byte[] bytesIn = new byte[1024];
					int read = 0;
					while ((read = zis.read(bytesIn)) != -1) {
						bos.write(bytesIn, 0, read);
					}
					bos.close();
					zis.closeEntry();

					String token = zipEntry.getName() + " " + currentTime + "_" +
							googleExposureClient.getExposureConfiguration().toString().hashCode();

					getView().post(() -> {
						View view = getLayoutInflater().inflate(R.layout.item_handshake, layout, false);
						((TextView) view.findViewById(R.id.device_name)).setText(token.substring(0, token.indexOf(' ')));
						layout.addView(view);
						view.setOnClickListener(v -> {
							TextView textView = view.findViewById(R.id.device_info);
							textView.setText("Loading...");
							new Thread(() -> {
								try {
									ArrayList<File> fileList = new ArrayList<>();
									fileList.add(file);
									googleExposureClient.provideDiagnosisKeys(fileList, token);
									Thread.sleep(2000);
									googleExposureClient.getExposureSummary(token)
											.addOnSuccessListener(exposureSummary ->
													view.post(() -> {
														textView.setText(exposureSummary.toString());
													}));
								} catch (Exception e) {
									e.printStackTrace();
									view.post(() -> {
										textView.setText("Exception: " + e.getLocalizedMessage());
									});
								}
							}).start();
						});
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

}
