/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.dpppt.android.calibration.controls.ControlsFragment;
import org.dpppt.android.calibration.handshakes.HandshakesFragment;
import org.dpppt.android.calibration.logs.LogsFragment;
import org.dpppt.android.calibration.parameters.ParametersFragment;
import org.dpppt.android.sdk.DP3T;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupNavigationView();

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.main_fragment_container, ControlsFragment.newInstance())
					.commit();
		}
	}

	private void setupNavigationView() {
		BottomNavigationView navigationView = findViewById(R.id.main_navigation_view);
		navigationView.inflateMenu(R.menu.menu_navigation_main);

		navigationView.setOnNavigationItemSelectedListener(item -> {
			switch (item.getItemId()) {
				case R.id.action_controls:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, ControlsFragment.newInstance())
							.commit();
					break;
				case R.id.action_parameters:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, ParametersFragment.newInstance())
							.commit();
					break;
				case R.id.action_handshakes:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, HandshakesFragment.newInstance())
							.commit();
					break;
				case R.id.action_logs:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, LogsFragment.newInstance())
							.commit();
					break;
			}
			return true;
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		boolean handled = DP3T.onActivityResult(this, requestCode, resultCode, data);

		if (!handled) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

}
