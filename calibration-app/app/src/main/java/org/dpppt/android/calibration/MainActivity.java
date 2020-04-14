/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.android.calibration;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.dpppt.android.calibration.controls.ControlsFragment;
import org.dpppt.android.calibration.handshakes.HandshakesFragment;
import org.dpppt.android.calibration.logs.LogsFragment;
import org.dpppt.android.calibration.parameters.ParametersFragment;

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

}
