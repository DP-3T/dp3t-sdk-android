/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 *
 * SPDX-FileCopyrightText: 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.internal.gatt;

import java.util.concurrent.LinkedBlockingQueue;

public class GattConnectionThread extends Thread {

	private boolean running = true;
	private LinkedBlockingQueue<GattConnectionTask> bluetoothDevicesToConnect = new LinkedBlockingQueue<>();

	public GattConnectionThread() {
		super("GattConnectionThread");
	}

	public void addTask(GattConnectionTask task) {
		bluetoothDevicesToConnect.add(task);
	}

	@Override
	public void run() {
		while (running) {
			GattConnectionTask task = null;
			try {
				task = bluetoothDevicesToConnect.take();
			} catch (InterruptedException e) {
				//ignore
			}
			if (task != null) {
				task.execute();
				while (!task.isFinished()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//ignore
					}
					if (running) {
						task.checkForTimeout();
					} else {
						task.finish();
					}
				}
			}
		}
	}

	public void terminate() {
		running = false;
		interrupt();
	}

}
