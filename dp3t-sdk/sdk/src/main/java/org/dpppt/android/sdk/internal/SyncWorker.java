/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import androidx.work.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.InvalidProtocolBufferException;

import org.dpppt.android.sdk.TracingStatus.ErrorState;
import org.dpppt.android.sdk.backend.SignatureException;
import org.dpppt.android.sdk.models.ApplicationInfo;
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository;
import org.dpppt.android.sdk.internal.backend.ServerTimeOffsetException;
import org.dpppt.android.sdk.internal.backend.StatusCodeException;
import org.dpppt.android.sdk.internal.backend.SyncErrorState;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.GoogleExposureClient;

import okhttp3.ResponseBody;

import static org.dpppt.android.sdk.internal.backend.BackendBucketRepository.BATCH_LENGTH;

public class SyncWorker extends Worker {

	private static final String TAG = "SyncWorker";
	private static final String WORK_TAG = "org.dpppt.android.sdk.internal.SyncWorker";

	private static PublicKey bucketSignaturePublicKey;

	public static void startSyncWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
				.setConstraints(constraints)
				.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
	}

	public static void stopSyncWorker(Context context) {
		WorkManager workManager = WorkManager.getInstance(context);
		workManager.cancelAllWorkByTag(WORK_TAG);
	}

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	public static void setBucketSignaturePublicKey(PublicKey publicKey) {
		bucketSignaturePublicKey = publicKey;
	}

	@NonNull
	@Override
	public Result doWork() {
		// TODO: check error state & publish notification if there is an issue

		Logger.d(TAG, "start SyncWorker");
		Context context = getApplicationContext();

		try {
			doSync(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.d(TAG, "SyncWorker finished with exception " + e.getMessage());
			return Result.retry();
		}
		Logger.d(TAG, "SyncWorker finished with success");
		return Result.success();
	}

	public static void doSync(Context context)
			throws IOException, StatusCodeException, ServerTimeOffsetException, SQLiteException, SignatureException {
		try {
			doSyncInternal(context);
			Logger.i(TAG, "synced");
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(true);
			SyncErrorState.getInstance().setSyncError(null);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
		} catch (IOException | StatusCodeException | ServerTimeOffsetException | SignatureException | SQLiteException e) {
			Logger.e(TAG, e);
			AppConfigManager.getInstance(context).setLastSyncNetworkSuccess(false);
			ErrorState syncError;
			if (e instanceof ServerTimeOffsetException) {
				syncError = ErrorState.SYNC_ERROR_TIMING;
			} else if (e instanceof SignatureException) {
				syncError = ErrorState.SYNC_ERROR_SIGNATURE;
			} else if (e instanceof StatusCodeException || e instanceof InvalidProtocolBufferException) {
				syncError = ErrorState.SYNC_ERROR_SERVER;
			} else if (e instanceof SQLiteException) {
				syncError = ErrorState.SYNC_ERROR_DATABASE;
			} else {
				syncError = ErrorState.SYNC_ERROR_NETWORK;
			}
			SyncErrorState.getInstance().setSyncError(syncError);
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			throw e;
		}
	}

	private static void doSyncInternal(Context context) throws IOException, StatusCodeException, ServerTimeOffsetException {
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		ApplicationInfo appConfig = appConfigManager.getAppConfig();

		long lastLoadedBatchReleaseTime = appConfigManager.getLastLoadedBatchReleaseTime();
		long nextBatchReleaseTime;
		if (lastLoadedBatchReleaseTime <= 0 || lastLoadedBatchReleaseTime % BATCH_LENGTH != 0) {
			long now = System.currentTimeMillis();
			nextBatchReleaseTime = now - (now % BATCH_LENGTH);
		} else {
			nextBatchReleaseTime = lastLoadedBatchReleaseTime + BATCH_LENGTH;
		}

		// TODO: debug code
		nextBatchReleaseTime -= 1 * BATCH_LENGTH;

		BackendBucketRepository backendBucketRepository =
				new BackendBucketRepository(context, appConfig.getBucketBaseUrl(), bucketSignaturePublicKey);

		GoogleExposureClient googleExposureClient = GoogleExposureClient.getInstance(context);

		long fixedBatchTime = 1589241600000l;

		googleExposureClient.getExposureSummary(googleExposureClient.getExposureConfiguration().toString() + fixedBatchTime)
				.addOnSuccessListener(exposureSummary -> Logger.d("result", exposureSummary.toString()))
				.addOnFailureListener(e -> e.printStackTrace());

		for (long batchReleaseTime = nextBatchReleaseTime;
			 batchReleaseTime < System.currentTimeMillis();
			 batchReleaseTime += BATCH_LENGTH) {

			ResponseBody result = backendBucketRepository.getGaenExposees(fixedBatchTime);

			/*GaenExposed.File newBatch = GaenExposed.File.newBuilder().addAllKey(result.getKeyList()).setHeader(
					GaenExposed.Header.newBuilder()
							.setBatchNum(0)
							.setBatchSize(1)
							.setRegion("ch")
							.setStartTimestamp(batchReleaseTime - BATCH_LENGTH)
							.setEndTimestamp(batchReleaseTime)
			).build();*/

			File file = new File(context.getFilesDir(), "keyList.zip");
			FileOutputStream fout = new FileOutputStream(file);
			fout.write(result.bytes());
			fout.flush();
			fout.close();

/*
			ZipFile zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			File file2 = new File(context.getFilesDir(), "keyList2.zip");
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file2));

			ZipEntry entry = zin.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				boolean notInFiles = true;
				for (File f : files) {
					if (f.getName().equals(name)) {
						notInFiles = false;
						break;
					}
				}
				if (notInFiles) {
					// Add ZIP entry to output stream.
				}
				entry = zin.getNextEntry();
			}
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if(entry.getName().equals("export.bin")){
					byte[] data = new byte[(int) entry.getSize()+16];
					byte[] header = "EK Export v1".getBytes();
					for(int i=0; i<12; i++){
						data[i] = header[i];
					}
					zipFile.getInputStream(entry).read(data, 16, data.length-16);

					out.putNextEntry(new ZipEntry(name));
					// Transfer bytes from the ZIP file to the output file
					int len;
					while ((len = zin.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
				}
			}
			zipFile.close();*/

			ArrayList<File> fileList = new ArrayList<>();
			fileList.add(file);
			googleExposureClient
					.provideDiagnosisKeys(fileList, googleExposureClient.getExposureConfiguration().toString() + fixedBatchTime);

			appConfigManager.setLastLoadedBatchReleaseTime(batchReleaseTime);
		}

		appConfigManager.setLastSyncDate(System.currentTimeMillis());
	}

}
