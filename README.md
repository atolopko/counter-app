# Android Counter App

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

## Testing
To run unit tests (if applicable):
```bash
./gradlew test
```
