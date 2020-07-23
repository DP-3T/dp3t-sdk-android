# How to use the calibration app for experiments

## Setup

Make sure that the calibration app is the only installed ExposureNotifications app (check Settings > Google > COVID-19 Exposure Notifications), otherwise clearing the exposure history is not possible over adb.

### Make sure your device supports ExposureNotification v1.5
Go to Settings > Google > COVID-19 exposure notifications and scroll to the bottom of the screen. Verify that the version number starts with 15... (Be aware that version 15xxxxxxxxx is newer than version 20xxxxxxx.)

Alternatively you can use the following adb command:
```
adb shell dumpsys activity provider com.google.android.gms.chimera.container.GmsModuleProvider | grep 'nearby_en'
```
This will show you the EN version, but here the leading 15 for v1.5 is not printed. Known v1.5 versions are: v203004001, v202902002

### Enable ExposureNotification Debug Mode
Go to Settings > Google > COVID-19 exposure notifications > Debug mode and make sure that the following options are enabled:
- Bypass app signature check
- Return all TEKs immediately
The option "Enable diagnosis key file signature check" should not be enabled.

### Reset app and exposure history
Run the following adb command to clear app and exposure history:
```
adb shell pm clear org.dpppt.android.calibration
```
Alternatively you can go to Settings > Apps > Calibration App and select "Clear data".

### Configure app
Set experiment name and device name with the following adb command (replace expName and devName):
```
adb shell am broadcast -a org.dpppt.android.calibration.adb --es experimentName "expName" --es deviceName "devName" -n org.dpppt.android.calibration/.handshakes.ADBBroadcastReceiver
```
Alternatively you can also open the app and enter experiment and device name in the parameters tab (second icon at the bottom).

Now you have a clean setup of the app where device and experiment name are already set.


## Run experiment
To start an experiment open the app and press the red "START TRACKING" button. An additional popup will show up where you have to agree to enable Exposure Notifications.

At the end of the experiment go to the parameters tab (second icon at the bottom) and press "UPLOAD KEYS FOR EXPERIMENT". Make sure that the parameters from the setup phase are set correctly.

To stop tracing and prevent any side-effects go back to the controls tab (first icon at the bottom) and press "STOP TRACKING".

## Execute matching

Make sure tracing is enabled on the device for the matching to work (this is a restriction of the Google ExposureNotification framework). To do this, go to the controls tab (first icon at the bottom) of the app and press "START TRACING" if not already active.

After all devices have uploaded their keys you have two options to execute the matching of a certain experiment:
### Matching with ADB
Run the following command to start the matching over adb (replace expName with the name of your experiment)
```
adb shell am broadcast -a org.dpppt.android.calibration.adb --es runMatching "expName" -n org.dpppt.android.calibration/.handshakes.ADBBroadcastReceiver
```
Check logcat to see when the matching finished (search for MatchingWorker), you will see the message "matching executed and uploaded successfully!".

### Matching from the app
Go to the handshakes tab (third icon at the bottom) of the app and you will see a list of all available experiments. Click an experiment to run the matching.
The result will be displayed in the app but also uploaded to the server.

### Check matching results
The matching result will be automatically uploaded as JSON to the following URL:
https://dp3tdemo.blob.core.windows.net/fileupload/result_experiment_{EXPERIMENT_NAME}_{DATE}_device_{DEVICE_NAME}.json
Example:
https://dp3tdemo.blob.core.windows.net/fileupload/result_experiment_1_2020-07-19_device_DD.json

## Good to know

- You can only execute one experiment and one (!) matching after a clean install. All subsequent experiments / matching runs will not work or lead to wrong results.

- Battery optimization does not have to be deactivated for experiments, this is only needed to guarantee periodic backend syncs in the background, which is irrelevant for experiments. The error notification can thus be ignored.