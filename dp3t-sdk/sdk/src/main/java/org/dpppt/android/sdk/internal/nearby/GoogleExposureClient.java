package org.dpppt.android.sdk.internal.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;

import java.util.List;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.exposurenotification.*;
import com.google.android.gms.tasks.Task;

import org.dpppt.android.sdk.internal.TracingController;
import org.dpppt.android.sdk.internal.logger.Logger;

public class GoogleExposureClient implements TracingController {

	private static final String TAG = "GoogleExposureClient";

	private static GoogleExposureClient instance;

	private final ExposureNotificationClient exposureNotificationClient;

	private ExposureConfiguration exposureConfiguration;

	public static GoogleExposureClient getInstance(Context context) {
		if (instance == null) {
			instance = new GoogleExposureClient(context);
		}
		return instance;
	}

	private GoogleExposureClient(Context context) {
		exposureNotificationClient = Nearby.getExposureNotificationClient(context.getApplicationContext());
	}

	@Override
	public void setParams(Bundle extras) {
		// TODO
		// default values
		exposureConfiguration = new ExposureConfiguration.ExposureConfigurationBuilder()
				.setMinimumRiskScore(4)
				.setAttenuationScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setAttenuationWeight(50)
				.setDaysSinceLastExposureScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setDaysSinceLastExposureWeight(50)
				.setDurationScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setDurationWeight(50)
				.setTransmissionRiskScores(new int[] { 4, 4, 4, 4, 4, 4, 4, 4 })
				.setTransmissionRiskWeight(50)
				.build();
	}

	public void startWithConfirmation(Activity activity, int requestCode) {
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}
		exposureNotificationClient.start(exposureConfiguration)
				.addOnSuccessListener(nothing -> Logger.i(TAG, "started successfully"))
				.addOnFailureListener(e -> {
					if (e instanceof ApiException) {
						ApiException apiException = (ApiException) e;
						if (apiException.getStatusCode() == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
							try {
								apiException.getStatus().startResolutionForResult(activity, requestCode);
								return;
							} catch (IntentSender.SendIntentException e2) {
								Logger.e(TAG, "Error calling startResolutionForResult, sending to settings");
							}
						}
					}
					Logger.e(TAG, e);
				});
	}

	@Override
	public void start() {
		if (exposureConfiguration == null) {
			throw new IllegalStateException("must call setParams()");
		}
		exposureNotificationClient.start(exposureConfiguration)
				.addOnSuccessListener(nothing -> Logger.i(TAG, "started successfully"))
				.addOnFailureListener(e -> {
					Logger.e(TAG, e);
					// TODO: add service error state
				});
	}

	@Override
	public void stop() {
		exposureNotificationClient.stop()
				.addOnSuccessListener(nothing -> Logger.i(TAG, "stopped successfully"))
				.addOnFailureListener(e -> {
					Logger.e(TAG, e);
					// TODO: add service error state
				});
	}

	public Task<Boolean> isEnabled() {
		return exposureNotificationClient.isEnabled();
	}

	public void getTemporaryExposureKeyHistory() {
		exposureNotificationClient.getTemporaryExposureKeyHistory()
				.addOnSuccessListener(temporaryExposureKeys -> Logger.i(TAG, "keys: " + temporaryExposureKeys.toString()))
				.addOnFailureListener(e -> Logger.e(TAG, e));
	}

	public Task<Void> provideDiagnosisKeys(List<TemporaryExposureKey> keys) {
		return exposureNotificationClient.provideDiagnosisKeys(keys);
	}

	public Task<Integer> getMaxDiagnosisKeysCount() {
		return exposureNotificationClient.getMaxDiagnosisKeyCount();
	}

	public Task<ExposureSummary> getExposureSummary() {
		return exposureNotificationClient.getExposureSummary();
	}

	public Task<List<ExposureInformation>> getExposureInformation() {
		return exposureNotificationClient.getExposureInformation();
	}

	public Task<Void> resetAllData() {
		return exposureNotificationClient.resetAllData();
	}

	public Task<Void> resetTemporaryExposureKey() {
		return exposureNotificationClient.resetTemporaryExposureKey();
	}

	@Override
	public void restartClient() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void restartServer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy() {
		instance = null;
	}

}
