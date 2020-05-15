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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dpppt.android.calibration.R;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;
import org.dpppt.android.sdk.models.DayDate;

import okhttp3.ResponseBody;

public class HandshakesFragment extends Fragment {

	private static final String TAG = "HandshakeFragment";

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

		new Thread(() -> {
			try {
				load();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (StatusCodeException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void load() throws IOException, StatusCodeException {
		Context context = getContext();
		if (context == null) {
			return;
		}
		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);
		long currentTime = System.currentTimeMillis();
		long batchReleaseTime = new DayDate().subtractDays(-1).getStartOfDayTimestamp();
		BackendUserBucketRepository backendBucketRepository = new BackendUserBucketRepository(context);
		ResponseBody result = backendBucketRepository.getGaenExposees(batchReleaseTime);

		ArrayList<String> tokens = new ArrayList<>();

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

			ArrayList<File> fileList = new ArrayList<>();
			fileList.add(file);
			String token = zipEntry.getName() + "_" + currentTime + "_" +
					googleExposureClient.getExposureConfiguration().toString().hashCode();
			googleExposureClient.provideDiagnosisKeys(fileList, token, e -> {
				Logger.e(TAG, e);
				// TODO: add service error state
			});
			tokens.add(token);
		}

		for (String token : tokens) {
			googleExposureClient.getExposureSummary(token).addOnSuccessListener(exposureSummary -> getView().post(() -> {
				TextView textView = getView().findViewById(R.id.handshake_list);
				String text = textView.getText().toString();
				text = text + "\n\n" + token + "\n" + exposureSummary.toString();
				textView.setText(text);
			}));
		}
	}

}
