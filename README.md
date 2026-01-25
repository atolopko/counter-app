# Count It!

A native Android application built with Jetpack Compose, Room, and Vico for tracking events and visualizing history.

## Features
- **Manual Counter Tracking**: Add, edit, and delete counters.
- **Persistent Storage**: Data is saved locally using Room Database.
- **Historical Data**: View a log of all increments for each counter.
- **Data Visualization**: Trend graphs for each counter using Vico Charting library.
- **Dark Mode Support**: Fully Material3 compliant.

## Technology Stack
- **Kotlin**: Primary language.
- **Jetpack Compose**: Modern UI toolkit.
- **Room**: Persistence layer.
- **MVVM**: Architecture pattern.
- **Vico**: Native Compose charting library.

## How to Run

### Prerequisites
1. **Android Studio**: Installed and updated (Hedgehog or later recommended).
2. **Android SDK**: API level 34.
3. **Emulator or Device**: Android 8.0 (API 26) or higher.

### Steps
1. **Open Project**: Launch Android Studio and select **Open**. Navigate to the root folder (`counter-app`).
2. **Sync Project**: Android Studio should automatically start syncing Gradle. If not, click **File > Sync Project with Gradle Files**.
3. **Run on Emulator/Device**:
    - Ensure an emulator is running or a physical device is connected via USB/ADB.
    - Click the **Run** button (Green Play icon) in the toolbar.
    - Select your device from the dropdown and click **OK**.
4. **Verification**:
    - Add a new counter using the FAB.
    - Tap the increment button (+10 by default) to record an event.
    - Click the **History** (clock icon) to view the graph and log list.

## Deploying to a Physical Phone

There are two main ways to get this app onto your real device:

### Option 1: Direct Install (via ADB/USB)
This is the standard developer method:
1. **Enable Developer Options**: On your phone, go to **Settings > About Phone** and tap **Build Number** 7 times.
2. **Enable USB Debugging**: Go to **Settings > Developer Options** and turn on **USB Debugging**.
3. **Connect to Mac**: Plug your phone into your computer via USB.
4. **Run from Android Studio**: Select your phone in the device dropdown list and click the green **Run** button.

### Option 2: Generate an APK (for manual transfer)
If you want to send the app file to your phone without a cable:
1. **Build APK**: In Android Studio, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. **Locate File**: Once the build finishes, click the **Locate** notification. The file will be at `app/build/outputs/apk/debug/app-debug.apk`.
3. **Transfer**: Send this `.apk` file to your phone via email, Google Drive, or Slack.
4. **Install**: Open the file on your phone. You may need to grant permission to "Install from Unknown Sources" in your phone's browser or file manager.

## Testing
To run unit tests:
```bash
./gradlew test
```

### Testing Backup and Restore

**Note**: The backup manager is enabled by default on end-user devices. The following commands are for local developer testing and validation only.

1. **Enable Backup Manager**:
   ```bash
   adb shell bmgr enabled
   ```
2. **Run a Manual Backup**:
   ```bash
   adb shell bmgr backupnow com.example.counterapp
   ```
3. **Simulate a Reinstall**:
   - Uninstall and then reinstall the app on your device.
   - Run the following command to restore the data:
   ```bash
   adb shell bmgr restore com.example.counterapp
   ```
