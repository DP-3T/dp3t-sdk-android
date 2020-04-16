/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.android.calibration.util.backend;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class FileUploadRepository {

	FileUpload uploadService;

	public FileUploadRepository() {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("https://dp3tdemoupload.azurewebsites.net/")
				.build();

		uploadService = retrofit.create(FileUpload.class);
	}

	public void uploadFile(File file, Callback<Void> callback) {
		RequestBody requestFile =
				RequestBody.create(
						MediaType.parse("application/sqlite"),
						file
				);
		MultipartBody.Part body =
				MultipartBody.Part.createFormData("file", file.getName(), requestFile);

		uploadService.upload(body).enqueue(callback);
	}

}
