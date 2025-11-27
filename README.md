# Assignment Application

Android app that scans installed applications and sends them to the DigitAR Media test API using Retrofit and WorkManager. Implemented entirely in **Java** with **XML** layouts.

## Tech Stack

- **Language**: Java
- **UI**: XML (no Jetpack Compose)
- **Architecture**: Simple layered structure with clear separation of concerns
  - `MainActivity` (UI / interaction entrypoint)
  - `AppScanner`, `TimeUtils`, `PreferencesManager` (helpers / domain logic)
  - `AppModel`, `PayloadModel` (data models)
  - `RetrofitClient`, `ApiService` (network layer)
  - `SendFullListWorker`, `SendNewInstallWorker` (background workers)
  - `PackageAddedReceiver` (BroadcastReceiver for app installs)
- **Networking**: Retrofit + OkHttp + HttpLoggingInterceptor
- **Async / Background Work**: WorkManager (Java API)
- **JSON Parsing**: Gson (via Retrofit converter)
- **Device ID**: `Settings.Secure.ANDROID_ID`
- **Local Storage**: SharedPreferences (wrapped by `PreferencesManager`)

## Features

- **Fetch all installed apps** using `PackageManager`.
  - Collects `app_name`, `package_name`, `installed_at` (formatted as `yyyy-MM-dd HH:mm:ss`).
- **Minimal UI**: Single screen with a **"Share App List"** button.
  - No list of installed apps is ever shown to the user.
- **Send full app list** on demand:
  - When the user taps **"Share App List"**, the app enqueues `SendFullListWorker`.
  - Worker builds payload:
    ```json
    {
      "device_id": "ANDROID_DEVICE_UNIQUE_ID",
      "sync_type": "full_list",
      "apps": [ { ... } ]
    }
    ```
  - POSTs to `https://api.digitarmedia.com/test/applist`.
- **Auto-detect new installations**:
  - `PackageAddedReceiver` listens for `ACTION_PACKAGE_ADDED` with `package` scheme.
  - When a new app is installed, it enqueues `SendNewInstallWorker` with the new package name.
  - Worker resolves app details and sends payload:
    ```json
    {
      "device_id": "ANDROID_DEVICE_UNIQUE_ID",
      "sync_type": "new_install",
      "apps": [ { ... } ]
    }
    ```
- **WorkManager constraints & retry**:
  - Both workers run only when `NetworkType.CONNECTED` is satisfied.
  - Network or HTTP failures return `Result.retry()` so WorkManager will retry with backoff.
- **Duplicate protection for new installs**:
  - `PreferencesManager` stores a set of package names already reported as `new_install`.
  - Before sending, `SendNewInstallWorker` checks if the package has been sent before; if yes, it exits successfully without re-sending.

## Project Structure

- `app/src/main/java/com/jetpack/assignmentapplication/`
  - `MainActivity.java`
  - `AppScanner.java`
  - `AppModel.java`
  - `PayloadModel.java`
  - `RetrofitClient.java`
  - `ApiService.java`
  - `SendFullListWorker.java`
  - `SendNewInstallWorker.java`
  - `PackageAddedReceiver.java`
  - `TimeUtils.java`
  - `PreferencesManager.java`
- `app/src/main/res/layout/`
  - `activity_main.xml` (single button)
- `app/src/main/AndroidManifest.xml`

## Setup & How to Run

1. **Open in Android Studio**
   - Open the root folder (`Assignment Application`) in Android Studio Giraffe or newer.
2. **Sync Gradle**
   - Let Gradle sync to download dependencies (Retrofit, OkHttp, Gson, WorkManager, etc.).
3. **Build & Run**
   - Connect a device or start an emulator (API 30+ recommended, as `QUERY_ALL_PACKAGES` is used).
   - Run the `app` configuration.
4. **Use the App**
   - On launch you see a single button **"Share App List"**.
   - Tap the button to trigger a full app list sync via WorkManager.
   - Install a new app on the device; `PackageAddedReceiver` will automatically trigger `SendNewInstallWorker` in the background when network is available.

## Permissions Explained

Declared in `AndroidManifest.xml`:

- `android.permission.INTERNET`
  - Required to send HTTP POST requests to `https://api.digitarmedia.com/test/applist`.
- `android.permission.QUERY_ALL_PACKAGES`
  - Required (on Android 11+) to access the full list of installed applications via `PackageManager`.
  - Without this, the app list would be filtered, breaking the assignment requirement.

*(Optional in assignment: `RECEIVE_BOOT_COMPLETED` is not used here; the app focuses on live session behavior and package-added events.)*

## Testing Instructions

1. **Full list sync**
   - Install and launch the app.
   - Press **"Share App List"**.
   - Verify in logs or via network proxy (e.g., Charles, mitmproxy) that:
     - A POST is sent to `https://api.digitarmedia.com/test/applist`.
     - Payload contains `sync_type = "full_list"`.
     - `apps` array contains installed apps with correctly formatted timestamps `yyyy-MM-dd HH:mm:ss`.
2. **New install sync**
   - With the app already installed (it does not have to be in foreground):
     - Install a new application on the device.
   - Confirm that `PackageAddedReceiver` fires and `SendNewInstallWorker` is enqueued (check Logcat tagged by WorkManager).
   - Verify that a POST is sent with `sync_type = "new_install"` and a single-element `apps` array.
   - Install the same app again (or clear data and re-test) and confirm SharedPreferences prevents duplicate "new_install" events.
3. **Offline / retry behavior**
   - Disable network before pressing **"Share App List"** or installing a new app.
   - Worker will return `Result.retry()`; when network becomes available, WorkManager will retry automatically.

## Play Store Restrictions for `QUERY_ALL_PACKAGES`

Google Play imposes strict policies on apps that request access to the full list of installed applications:

- Only apps whose **core functionality** requires broad package visibility are allowed.
- Example acceptable categories (as per Play policy hints):
  - Antivirus / Security apps
  - Device management / Parental control
  - App locker / Privacy locker
  - Digital wellbeing / Usage monitoring
  - Backup & restore tools
  - Launcher apps
  - Phone clone / Device migration tools
- For production, the app would need to clearly justify why it needs `QUERY_ALL_PACKAGES` and describe this in the Play Console declaration form.

This project uses `QUERY_ALL_PACKAGES` **only to satisfy the assignment requirement** of scanning and sending the complete list of installed applications.
