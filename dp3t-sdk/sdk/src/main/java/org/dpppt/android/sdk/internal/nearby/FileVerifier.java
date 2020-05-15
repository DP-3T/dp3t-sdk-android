/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.nearby;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileVerifier {

	public static void verify(File file) throws IOException {

		ZipFile zipFile = new ZipFile(file);
		if (zipFile.getEntry("export.bin") == null) {
			throw new IllegalStateException("export.bin missing in ZipFile");
		}
		if (zipFile.getEntry("export.sig") == null) {
			throw new IllegalStateException("export.sig missing in ZipFile");
		}

		ZipEntry binEntry = zipFile.getEntry("export.bin");
		byte[] data = new byte[(int) binEntry.getSize()];
		zipFile.getInputStream(binEntry).read(data);
		if (!new String(data).startsWith("EK Export v1    ")) {
			throw new IllegalStateException("export.bin has not the correct header");
		}
	}

}
