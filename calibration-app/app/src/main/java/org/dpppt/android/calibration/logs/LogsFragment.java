/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.calibration.logs;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dpppt.android.sdk.internal.logger.LogEntry;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.calibration.R;
import org.dpppt.android.calibration.util.OnTextChangedListener;

public class LogsFragment extends Fragment {

	private RecyclerView logsList;
	private LinearLayoutManager layoutManager;
	private LogsAdapter logsAdapter;

	private Handler handler = new Handler();
	private Runnable updateLogsRunnable;

	private final List<LogLevel> logLevels = Arrays.asList(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR);

	public static LogsFragment newInstance() {
		return new LogsFragment();
	}

	public LogsFragment() {
		super(R.layout.fragment_logs);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		logsList = view.findViewById(R.id.logs_list);
		layoutManager = (LinearLayoutManager) logsList.getLayoutManager();
		logsAdapter = new LogsAdapter(getContext());
		logsList.setAdapter(logsAdapter);

		Spinner spinner = view.findViewById(R.id.logs_filter_level);
		List<String> logLevelValues = new ArrayList<>(logLevels.size());
		for (LogLevel logLevel : logLevels) {
			logLevelValues.add(logLevel.getValue());
		}
		ArrayAdapter<String> logLevelSpinnerAdapter =
				new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, logLevelValues);
		logLevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(logLevelSpinnerAdapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				logsAdapter.setFilterLogLevel(logLevels.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		MultiAutoCompleteTextView tagFilterInput = view.findViewById(R.id.logs_filter_tag);

		ArrayAdapter<String> tagAutocompletionAdapter =
				new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, Logger.getTags());
		tagFilterInput.setAdapter(tagAutocompletionAdapter);
		tagFilterInput.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		tagFilterInput.addTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String[] tags = tagFilterInput.getText().toString().split("\\s*,\\s*");
				logsAdapter.setFilterTags(Arrays.asList(tags));
			}
		});

		view.findViewById(R.id.logs_scrolltobottom).setOnClickListener(v -> {
			if (logsAdapter.getItemCount() > 0) {
				logsList.smoothScrollToPosition(logsAdapter.getItemCount() - 1);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		updateLogsRunnable = () -> {
			boolean isAtBottom = layoutManager.findLastCompletelyVisibleItemPosition() == logsAdapter.getItemCount() - 1;

			List<LogEntry> logs = Logger.getLogs(logsAdapter.getLastLogTime() + 1);
			logsAdapter.appendLogs(logs);

			if (isAtBottom && logsAdapter.getItemCount() > 0) {
				logsList.smoothScrollToPosition(logsAdapter.getItemCount() - 1);
			}

			handler.postDelayed(updateLogsRunnable, 2 * 1000L);
		};
		updateLogsRunnable.run();
	}

	@Override
	public void onStop() {
		super.onStop();

		handler.removeCallbacks(updateLogsRunnable);
	}

}
