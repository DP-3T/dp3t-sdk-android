/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.sdk.internal.backend;

import androidx.annotation.NonNull;

import okhttp3.CertificatePinner;

public class CertificatePinning {

	private static CertificatePinner certificatePinner = CertificatePinner.DEFAULT;

	public static void setCertificatePinner(@NonNull CertificatePinner certificatePinner) {
		CertificatePinning.certificatePinner = certificatePinner;
	}

	public static CertificatePinner getCertificatePinner() {
		return certificatePinner;
	}

}
