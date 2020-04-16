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

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.*;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.database.models.Handshake;
import org.dpppt.android.calibration.R;

public class HandshakesFragment extends Fragment {

	private static final int MAX_NUMBER_OF_MISSING_HANDSHAKES = 3;

	Switch rawHandshakeSwitch;
	TextView handshakeList;

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

		rawHandshakeSwitch = view.findViewById(R.id.raw_handshake_switch);
		handshakeList = view.findViewById(R.id.handshake_list);

		loadHandshakes(rawHandshakeSwitch.isChecked());

		rawHandshakeSwitch.setOnCheckedChangeListener((compoundButton, raw) -> loadHandshakes(raw));

		view.findViewById(R.id.refresh).setOnClickListener((v) -> {
			loadHandshakes(rawHandshakeSwitch.isChecked());
		});
	}

	private void loadHandshakes(boolean raw) {
		handshakeList.setText("Loading...");
		new Database(getContext()).getHandshakes(response -> {
			StringBuilder stringBuilder = new StringBuilder();
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm:ss");
			if (raw) {
				Collections.sort(response, (h1, h2) -> Long.compare(h2.getTimestamp(), h1.getTimestamp()));
				for (Handshake handShake : response) {
					stringBuilder.append(sdf.format(new Date(handShake.getTimestamp())));
					stringBuilder.append(" ");
					stringBuilder.append(new String(Base64.encode(handShake.getEphId(), Base64.NO_WRAP)).substring(0, 10));
					stringBuilder.append("...");
					stringBuilder.append(" TxPowerLevel: ");
					stringBuilder.append(handShake.getTxPowerLevel());
					stringBuilder.append(" RSSI:");
					stringBuilder.append(handShake.getRssi());
					stringBuilder.append("\n");
				}
			} else {
				for (HandshakeInterval interval : mergeHandshakes(response)) {
					stringBuilder.append(sdf.format(new Date(interval.starttime)));
					stringBuilder.append(" - ");
					stringBuilder.append(sdf.format(new Date(interval.endtime)));
					stringBuilder.append("\n");
					stringBuilder.append(interval.identifier);
					stringBuilder.append(" Handshakes: ");
					stringBuilder.append(interval.count);
					stringBuilder.append(" / ");
					stringBuilder.append(interval.expectedCount);
					stringBuilder.append("\n\n");
				}
			}
			handshakeList.setText(stringBuilder.toString());
		});
	}

	private List<HandshakeInterval> mergeHandshakes(List<Handshake> handshakes) {

		HashMap<String, List<Handshake>> groupedHandshakes = new HashMap<>();
		for (Handshake handshake : handshakes) {
			byte[] head = new byte[4];
			for (int i = 0; i < 4; i++) {
				head[i] = handshake.getEphId()[i];
			}
			String identifier = new String(head);
			if (!groupedHandshakes.containsKey(identifier)) {
				groupedHandshakes.put(identifier, new ArrayList<>());
			}
			groupedHandshakes.get(identifier).add(handshake);
		}

		long scanInterval = AppConfigManager.getInstance(getContext()).getScanInterval();
		List<HandshakeInterval> result = new ArrayList<>();
		for (Map.Entry<String, List<Handshake>> entry : groupedHandshakes.entrySet()) {
			Collections.sort(entry.getValue(), (h1, h2) -> Long.compare(h1.getTimestamp(), h2.getTimestamp()));
			int start = 0;
			int end = 1;
			while (end < entry.getValue().size()) {
				if (entry.getValue().get(end).getTimestamp() - entry.getValue().get(end - 1).getTimestamp() >
						MAX_NUMBER_OF_MISSING_HANDSHAKES * scanInterval) {
					HandshakeInterval interval = new HandshakeInterval();
					interval.identifier = entry.getKey();
					interval.starttime = entry.getValue().get(start).getTimestamp();
					interval.endtime = entry.getValue().get(end - 1).getTimestamp();
					interval.count = end - start;
					interval.expectedCount =
							1 + (int) Math.ceil((interval.endtime - interval.starttime) * 1.0 / scanInterval);
					result.add(interval);
					start = end;
				}
				end++;
			}

			HandshakeInterval interval = new HandshakeInterval();
			interval.identifier = entry.getKey();
			interval.starttime = entry.getValue().get(start).getTimestamp();
			interval.endtime = entry.getValue().get(end - 1).getTimestamp();
			interval.count = end - start;
			interval.expectedCount =
					1 + (int) Math.ceil((interval.endtime - interval.starttime) * 1.0 / scanInterval);
			result.add(interval);
		}

		Collections.sort(result, (i1, i2) -> Long.compare(i2.endtime, i1.endtime));

		return result;
	}

	private class HandshakeInterval {
		String identifier;
		long starttime;
		long endtime;
		int count;
		int expectedCount;

	}

}
