# Changelog for DP3T-SDK Android

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
