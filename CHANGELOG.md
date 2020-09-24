# Changelog for DP3T-SDK Android

## version 1.0.5 (24.9.2020)

- support location less scanning on Android 11
- fix state update bug when EN are activated/deactivated outside of the app
- update targetSdk to 30, use gradle 6.6.1 and Android-Plugin 4.0.1
- fix https://github.com/DP-3T/dp3t-sdk-android/issues/206

## version 1.0.4 (25.8.2020)

- exposed days are now deleted 14 days after reporting the exposureDay (before they were deleted 10 days after the exposure which could be only 1 day after reporting)
- handle same day release of TEK: if we receive the current days TEK directly, we do a fake request the next day. Otherwise, the current days TEK is uploaded the next day as before.
- the number of uploaded keys has been increased to 30, because with same day release of keys it is possible to have more than 14 TEKs for the past 14 days

## version 1.0.3 (24.7.2020)

- upgraded to play-services-nearby-18.0.3-eap (adds a service for Google to restart our application after force stop starting v1.5)
- add success and error callback to senFakeInfectedRequest
- add response body to StatusCodeException

## version 1.0.2 (17.7.2020)

- fix logic for when a sync is considered successfull in HistoryDatabase (only if no errors)

## version 1.0.1 (16.7.2020)

- less frequent error notifications (only once per error while errors persist, wait 5min for GPS/Bluetooth state changes, wait 24h before showing EN API Errors)
- prevent rate limit errors when EN Api returns an error
- do not show http response code 504 error directly (like we already did for 502 and 503)
- fixed serialization issue in combination with core library desugaring

## version 1.0.0 (18.6.2020)

- prevent sync from running when ExposureNotifications disabled
- updated readme

## Version 0.6.0 (17.6.2020)

- additional error state for not enough disc space

## Version 0.5.7 (16.6.2020)

- improve sync logic to prevent rate limit issues with google api (sync always at 6am/6pm Swiss time!)
- schedule sync tasks only every two hours to match iOS logic
- only return latest exposure date to the app
- do not show statuscode error 502 and 503 directly
- make fake data in exposed requests more realistic

## Version 0.5.6 (12.6.2020)

- increased time delta check to 10min

## Version 0.5.5 (11.6.2020)

- improve history logging

## Version 0.5.4 (11.6.2020)

- bugfix for user-visible logs

## Version 0.5.3 (11.6.2020)

- new sync error state for ssl/tls errors
- allow export of log database in calibration flavor
- add database for user-visible logs (event history)

## Version 0.5.2 (3.6.2020)

- more logs
- code cleanup

## Version 0.5.1 (27.5.2020)

- reduce number of sync requests and prevent rate limit issue
- improved error logging

## Version 0.5.0 (22.5.2020)

- cleanup keyfiles after insertion

## Version 0.4.4 (22.5.2020)

- do not show sync error if syncing/tracing is disabled
- set sync times to 6am and 6pm local time

## Version 0.4.3 (22.5.2020)

- more flexibility for errorState

## Version 0.4.2 (21.5.2020)

- add new formula for exposure detection

## Version 0.4.1 (21.5.2020)

- improved error notification handling
- make sure exposure days are only flagged as deleted to prevent renotification

## Version 0.4.0 (19.5.2020)

- switched to GAEN framework

## Version 0.2.6 (8.5.2020)

- Change signing of test calibration app
- Make location hardware optional

## Version 0.2.5 (7.5.2020)

- updated contact logic window size to 5min

## Version 0.2.4 (6.5.2020)

- simplified contact logic and increased threshold

## Version 0.2.3 (4.5.2020)

- Change 16bit UUID to DP3T registered FD68

## Version 0.2.2 (4.5.2020)

- improved sync signature exception handling

## Version 0.2.1 (1.5.2020)

- Add logs to sync worker

## Version 0.2.0 (30.4.2020)

- Option to pin TLS certificates of backend endpoints

## Version 0.1.16 (30.4.2020)

- provide a public key to authenticate bucket response

## Version 0.1.15 (30.4.2020)

- stop tracing after reporting positive
- make contact logic parameters configurable

## Version 0.1.14 (29.4.2020)

- fix attenuation calculation

## Version 0.1.13 (29.4.2020)

- revert premature dependency update

## Version 0.1.12 (29.4.2020)

- Switch default advertise mode to BALANCED

## Version 0.1.11 (29.4.2020)

- Change exposure logic to count all exposure windows per day (breaking database change, needs reinstall)
- use attenuation for thresholds instead of rssi

## Version 0.1.10 (28.4.2020)

- improved error handling for sync requests
- add device info to calibration database

## Version 0.1.9 (27.4.2020)

- add log entry for every handshake (ephId) received
- use timestamp instead of DayDate to report key to backend
- make sure location services are enabled and report error otherwise, needed for BLE scanning to work

## Version 0.1.8 (25.4.2020)

- Add more options to Calibration app to test Bluetooth settings
- Set all default scan and advertise settings explicitly
- Show Version in Calibration app Parameters tab
- Add Changelog file
