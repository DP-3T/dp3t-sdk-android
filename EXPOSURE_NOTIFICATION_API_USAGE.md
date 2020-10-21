# ExposureNotification API usage
This document outlines the interaction of the SDK with the [Exposure Notification](https://www.google.com/covid19/exposurenotifications/) Framework v1.5 by Google.

## Enabling Exposure Notifications

To enable Exposure Notifications for our app we need to call `exposureNotificationClient.start()`. This will trigger a system popup asking the user to either enable Exposure Notifications on this device or (if another app is active) to switch to our app as active Exposure Notifications app. If this popup needs to be shown to the user, the `start()` method results in an ApiException that allows us to call `startResolutionForResult()` to trigger the system popup. The result (user accepts or declines) is then returned as an activity result intent.

## Disabling Exposure Notifications

To disable Exposure Notifications for our app we need to call `exposureNotificationClient.stop()`. This will stop the BLE broadcasting and scanning until in our app (or another Exposure Notifications app) `start()` is called again.

## Check if Exposure Notifications enabled

To check if Exposure Notifications are still enabled for our app (the user could have activated them in another app and thus deactivated them for our app) we need to call `exposureNotificationClient.isEnabled()`.

## Exporting Temporary Exposure Keys

To retrieve the Temporary Exposure Keys (TEKs) we need to call `exposureNotificationClient.getTemporaryExposureKeyHistory()`. This will result in an `ApiException` if Exposure Notifications are disabled. If enabled the method results in an `ApiException` that allows us to call `startResolutionForResult()` to trigger a system popup asking the user if he wants to share the TEKs of the last 14 days with the app. The result (user accepts or declines in the popup) is then returned as an activity result intent. If the user agrees to share the keys with the app in the popup the next call to `getTemporaryExposureKeyHistory()` will return the TEKs directly without an additional Exception or system popup.

There are two possibilities how the TEK of the current day can be retrieved. Which case is currently active is decided by a configuration of Google.
### Same day TEK release with shortened rolling period
In this case, the TEK of the current day and the previous 13 days are returned by `getTemporaryExposureKeyHistory()`. The TEK of the current day has a rolling period that is shorter than 24h, so this key is no longer valid after it was returned. If the device continues to have exposure notifications active a new TEK will be used on the same day. Therefore, it is possible that for the same date multiple TEKs are returned by `getTemporaryExposureKeyHistory()`, if the user allowed to export the keys already once or more in the previous 14 days.

### Next day TEK release
In this case, the TEK of the current day is not returned by `getTemporaryExposureKeyHistory()`, but only the keys of the previous 13 days. After the user agreed to share the keys we can call `getTemporaryExposureKeyHistory()` again on the following day and will then receive the TEK of the day the user agreed to share the keys as well. For this to work, Exposure Notifications must still be active for our app.

## Detecting Exposure

For a contact to be counted as a possible exposure it must be longer than a certain number of minutes on a certain day. The current implementation of the EN-framework does not expose this information directly, but as of version 1.5 of the exposure notification framework, we can use the new exposure windows feature to calculate the information ourselves.

To check for exposure we need to call `exposureNotificationClient.provideDiagnosisKeys()`. This method has only one parameter and takes a file list containing the TEKs.

These TEKs to check for exposure against must be provided in a [special file format](https://developers.google.com/android/exposure-notifications/exposure-key-file-format). The API would allow for multiple files being provided, but we always provide all available keys in a single file.


### Result

The result of the `provideDiagnosisKeys()` call is provided as a broadcast with action `ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED`. After this we can call [`getExposureWindows()`](https://developers.google.com/android/exposure-notifications/exposure-notifications-api#exposurewindow) to get a list of `ExposureWindows` describing our current state of exposure.


#### Calculation of exposure from ExposureWindows
A `ExposureWindow` is a set of Bluetooth scan events from observed beacons within a timespan. A window contains multiple `ScanInstance` which are aggregations of attenuation of beacons during a scan.

By grouping the ExposureWindows by day and then adding up all `secondsSinceLastScan` where `typicalAttenuationDb` lies between our defined attenuation thresholds we can compose three buckets.

The thresholds for the attenuation buckets are loaded from our [config server](https://github.com/DP-3T/dp3t-config-backend-ch/blob/master/dpppt-config-backend/src/main/java/org/dpppt/switzerland/backend/sdk/config/ws/model/GAENSDKConfig.java).

To detect an exposure the following formula is used to compute the exposure duration:
```
durationAttenuationLow * factorLow + durationAtttenuationMedium * factorMedium
```

### Rate limit

We are only allowed to call `provideDiagnosisKeys()` 6 times per UTC day. Therefore, after an attempted call to `provideDiagnosisKeys()` we always wait 4h before doing the next call to guarantee to stay within the rate limit.
