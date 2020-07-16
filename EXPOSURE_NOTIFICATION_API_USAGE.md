# ExposureNotification API usage
This document outlines the interaction of the SDK with the [Exposure Notification](https://www.google.com/covid19/exposurenotifications/) Framework by Google.

## Enabling Exposure Notifications

To enable Exposure Notifications for our app we need to call `exposureNotificationClient.start()`. This will trigger a system popup asking the user to either enable Exposure Notifications on this device or (if another app is active) to switch to our app as active Exposure Notifications app. If this popup needs to be shown to the user, the `start()` method results in an ApiException that allows us to call `startResolutionForResult()` to trigger the system popup. The result (user accepts or declines) is then returned as an activity result intent.

## Disabling Exposure Notifications

To disable Exposure Notifications for our app we need to call `exposureNotificationClient.stop()`. This will stop the BLE broadcasting and scanning until in our app (or another Exposure Notifications app) `start()` is called again.

## Check if Exposure Notifications enabled

To check if Exposure Notifications are still enabled for our app (the user could have activated them in another app and thus deactivated them for our app) we need to call `exposureNotificationClient.isEnabled()`.

## Exporting Temporary Exposure Keys

To retrieve the Temporary Exposure Keys (TEKs) we need to call `exposureNotificationClient.getTemporaryExposureKeyHistory()`. This will result in an `ApiException` if Exposure Notifications are disabled. If enabled the method results in an `ApiException` that allows us to call `startResolutionForResult()` to trigger a system popup asking the user if he wants to share the TEKs of the last 14 days with the app. The result (user accepts or declines in the popup) is then returned as an activity result intent. If the user agrees to share the keys with the app in the popup the next call to `getTemporaryExposureKeyHistory()` will return the TEKs directly without an additional Exception or system popup.

The TEK of the current day is never returned by `getTemporaryExposureKeyHistory()`, but only the keys of the previous 13 days. After the user agreed to share the keys we can call `getTemporaryExposureKeyHistory()` again on the following day and will then receive the TEK of the day the user agreed to share the keys as well. For this to work, Exposure Notifications must still be active for our app.

## Detecting Exposure

For a contact to be counted as a possible exposure it must be longer than a certain number of minutes on a certain day. The current implementation of the EN-framework does not expose this information. Our way to overcome this limitation is to pass the published keys for each day individually to the framework.

To check for exposure on a given day (we check the past 10 days) we need to call `exposureNotificationClient.provideDiagnosisKeys()`. This method has three parameters:

#### File list
TEKs to check for exposure against must be provided in a [special file format](https://developers.google.com/android/exposure-notifications/exposure-key-file-format). The API would allow for multiple files being provided, but we always provide all available keys in a single file.

#### Exposure Configuration
The exposure configuration defines the configuration for the Google scoring of exposures. In our case we ignore most of the scoring methods and only provide the thresholds for the duration at attenuation buckets. The thresholds for the attenuation buckets are loaded from our [config server](https://github.com/DP-3T/dp3t-config-backend-ch/blob/master/dpppt-config-backend/src/main/java/org/dpppt/switzerland/backend/sdk/config/ws/model/GAENSDKConfig.java). This allows us to group the duration of a contact with another device into three buckets regarding the measured attenuation values that we then use to detect if the contact was long enough and close enough.
To detect an exposure the following formula is used to compute the exposure duration:
```
durationAttenuationLow * factorLow + durationAttenuationMedium * factorMedium
```
If this duration is at least as much as defined in the `triggerThreshold` a notification is triggered for that day.

#### Token
Providing a token allows us to update an exposure check executed previously and only providing additional new TEKs in the file. The previously provided TEKs for the same token are stored internally by the framework and the new exposure result is the total exposure with all provided TEKs in the current and previous calls. When we download TEKs from our backend we receive a token (timestamp) that we can use for the next sync to only download the newly added TEKs. This reduces traffic between the app and the backend.

### Result

The result of the `provideDiagnosisKeys()` call is provided as a broadcast with action `ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED`. In the Intent we directly get the `ExposureSummary` object, that allows us to check if the exposure limit for a notification was reached by checking the minutes of exposure per attenuation window. The duration per window has a maximum of 30 minutes, longer exposures are also returned as 30 minutes of exposure.

### Rate limit

We are only allowed to call `provideDiagnosisKeys()` 20 times per UTC day. Because we check for every of the past 10 days individually, this allows us to check for exposure at most twice per day. These checks happen after 6am and 6pm (swiss time) when the SyncWorker is scheduled the next time or the app is opened. All 10 days are checked individually and if one fails it is retried on the next run. No checks are made between midnight UTC and 6am (swiss time) to prevent exceeding the rate limit per UTC day.
