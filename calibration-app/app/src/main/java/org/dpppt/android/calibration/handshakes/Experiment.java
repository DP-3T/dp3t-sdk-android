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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Experiment {

	String name;
	List<Device> devices;

	public Experiment(String name) {
		this.name = name;
		this.devices = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public static class Device {
		String name;
		File file;

		public Device(String name, File file) {
			this.name = name;
			this.file = file;
		}

		public String getName() {
			return name;
		}

		public File getFile() {
			return file;
		}

	}

}
