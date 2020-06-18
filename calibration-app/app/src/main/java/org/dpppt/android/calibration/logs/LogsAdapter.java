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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

import org.dpppt.android.sdk.internal.logger.LogEntry;
import org.dpppt.android.sdk.internal.logger.LogLevel;
import org.dpppt.android.calibration.R;

class LogsAdapter extends RecyclerView.Adapter<LogsViewHolder> {

	private final LayoutInflater inflater;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);

	private final List<LogEntry> logs = new ArrayList<>();
	private List<LogEntry> filteredLogs = new ArrayList<>();

	private LogLevel filterLogLevel = LogLevel.DEBUG;
	private final Set<String> filterTags = new HashSet<>();

	public LogsAdapter(Context context) {
		inflater = LayoutInflater.from(context);
	}

	@NonNull
	@Override
	public LogsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new LogsViewHolder(inflater.inflate(R.layout.view_log_entry, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull LogsViewHolder holder, int position) {
		LogEntry logEntry = filteredLogs.get(position);
		holder.timeView.setText(dateFormat.format(new Date(logEntry.getTime())));
		holder.levelView.setText(logEntry.getLevel().getKey());
		holder.tagView.setText(logEntry.getTag());
		holder.messageView.setText(logEntry.getMessage());

		int color = 0x22000000;
		switch (logEntry.getLevel()) {
			case DEBUG:
				color |= 0x0000FF;
				break;
			case INFO:
				color |= 0x00AA00;
				break;
			case WARNING:
				color |= 0xFFAA00;
				break;
			case ERROR:
				color |= 0xFF0000;
				break;
		}
		holder.itemView.setBackgroundColor(color);
	}

	@Override
	public int getItemCount() {
		return filteredLogs.size();
	}

	public long getLastLogTime() {
		if (logs.isEmpty()) {
			return -1;
		} else {
			return logs.get(logs.size() - 1).getTime();
		}
	}

	public void appendLogs(List<LogEntry> logEntries) {
		logs.addAll(logEntries);

		int startIndex = filteredLogs.size();
		List<LogEntry> filteredLogsToAdd = getFilteredLogs(logEntries);
		filteredLogs.addAll(filteredLogsToAdd);

		notifyItemRangeInserted(startIndex, filteredLogsToAdd.size());
	}

	public void setFilterLogLevel(LogLevel logLevel) {
		filterLogLevel = logLevel;
		invalidateFilteredList();
	}

	public void setFilterTags(List<String> tags) {
		filterTags.clear();
		filterTags.addAll(tags);
		filterTags.remove("");
		invalidateFilteredList();
	}

	private void invalidateFilteredList() {
		filteredLogs = getFilteredLogs(logs);
		notifyDataSetChanged();
	}

	private List<LogEntry> getFilteredLogs(List<LogEntry> logs) {
		List<LogEntry> filteredLogs = new ArrayList<>();
		for (LogEntry log : logs) {
			if (log.getLevel().getImportance() >= filterLogLevel.getImportance()) {
				if (filterTags.isEmpty() || filterTags.contains(log.getTag())) {
					filteredLogs.add(log);
				}
			}
		}
		return filteredLogs;
	}

}

class LogsViewHolder extends RecyclerView.ViewHolder {

	final TextView timeView;
	final TextView levelView;
	final TextView tagView;
	final TextView messageView;

	LogsViewHolder(@NonNull View itemView) {
		super(itemView);
		timeView = itemView.findViewById(R.id.log_entry_time);
		levelView = itemView.findViewById(R.id.log_entry_level);
		tagView = itemView.findViewById(R.id.log_entry_tag);
		messageView = itemView.findViewById(R.id.log_entry_msg);
	}

}
