# Changelog for DP3T-SDK Android

## Latest

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
