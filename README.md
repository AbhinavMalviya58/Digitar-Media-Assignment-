# Assignment Application – App List Sync (Android, Java)

## 1. Project Overview

This project is an Android app written in **Java + XML** that reads the list of installed apps on the device and sends that data to a backend API.

There is **no screen that shows the app list**. The only visible element is a single button: **“Share App List”**.  
All syncing is done in the background using **WorkManager**, **Retrofit**, and a **BroadcastReceiver** for new installs.

---

## 2. What the app does (as per assignment)

This app strictly follows the PDF requirements:

- **Fetch all installed apps** from the device using `PackageManager`.
- **Do not display** the installed app list to the user anywhere in the UI.
- **Automatically send the full app list** to the API whenever the app opens.
- **Provide a “Share App List” button** that also triggers a full app list sync on demand.
- **Detect new app installations** using `ACTION_PACKAGE_ADDED` and send **only the newly installed app** to the API.
- Send data to the API endpoint:

  - Method: `POST`
  - URL: `https://api.digitarmedia.com/test/applist`
  - `Content-Type: application/json`

- Use `sync_type` to distinguish:

  - `sync_type = "full_list"` – when sending the full installed app list.
  - `sync_type = "new_install"` – when sending a single newly installed app.

- For each app, include:

  - `app_name`
  - `package_name`
  - `installed_at` formatted as `yyyy-MM-dd HH:mm:ss`.

- Use the device’s `ANDROID_ID` as `device_id`.
- Use `QUERY_ALL_PACKAGES` because the assignment requires access to the full app list.

---

## 3. How the app works technically

At a high level:

- **MainActivity**
  - On app launch (`onCreate`), it immediately enqueues a WorkManager job to send the full app list (`sync_type = "full_list"`).
  - The **“Share App List”** button calls the same WorkManager logic again, so the user can manually re-sync.

- **WorkManager**
  - `SendFullListWorker`:
    - Scans all installed apps.
    - Builds the JSON payload.
    - Calls the API using Retrofit.
    - Returns the API result back to `MainActivity` via WorkManager output.
  - `SendNewInstallWorker`:
    - Runs when a new app is installed.
    - Looks up only that specific package.
    - Sends a `new_install` payload for that one app.
    - Uses `SharedPreferences` to avoid sending the same package multiple times.

- **Networking**
  - `Retrofit` + `OkHttp` + `Gson` are used to call the `POST` API and serialize/deserialize JSON.
  - `ApiService` defines `sendAppList(...)`.
  - `ApiResponseModel` maps the API’s response fields:
    - `success: boolean`
    - `message: string`

- **Device ID**
  - `device_id` is taken from `Settings.Secure.ANDROID_ID`.

- **Error / success feedback**
  - When a full-list sync finishes, `MainActivity` observes the WorkManager result.
  - If `success = true`, it shows a Toast: **“App list synced successfully”**.
  - If `success = false` or the server sends message text (for example, `"Only POST allowed"`), that message is shown to the user in a Toast.

---

## 4. JSON payload examples

### 4.1 Full list payload (`sync_type = "full_list"`)

```json
{
  "device_id": "ANDROID_DEVICE_UNIQUE_ID",
  "sync_type": "full_list",
  "apps": [
    {
      "app_name": "Phone",
      "package_name": "com.android.dialer",
      "installed_at": "2025-11-28 01:53:00"
    },
    {
      "app_name": "Gmail",
      "package_name": "com.google.android.gm",
      "installed_at": "2025-11-27 10:15:42"
    }
  ]
}
```

### 4.2 New install payload (`sync_type = "new_install"`)

```json
{
  "device_id": "ANDROID_DEVICE_UNIQUE_ID",
  "sync_type": "new_install",
  "apps": [
    {
      "app_name": "Some New App",
      "package_name": "com.example.newapp",
      "installed_at": "2025-11-28 02:10:05"
    }
  ]
}
```

The structure is the same; only `sync_type` and the size of the `apps` array differ.

---

## 5. How new app installation detection works

- The app registers a `BroadcastReceiver` (`PackageAddedReceiver`) in `AndroidManifest.xml`.
- It listens for:

  ```xml
  <action android:name="android.intent.action.PACKAGE_ADDED" />
  <data android:scheme="package" />
  ```

- When a new app is installed:
  - The receiver is triggered with the new package’s name.
  - It enqueues a `SendNewInstallWorker` with that package name as input.
  - The worker:
    - Checks if this package was already sent (using `PreferencesManager` and `SharedPreferences`).
    - If not sent before, it loads that app’s info and makes a `new_install` API call.
    - After a successful call, the package name is stored in shared preferences to prevent duplicates.

---

## 6. How the auto-sync on app launch works

- In `MainActivity.onCreate()`:
  - After `setContentView(...)` and basic view setup, `enqueueFullListWork()` is called.
- `enqueueFullListWork()`:
  - Builds a `OneTimeWorkRequest` for `SendFullListWorker`.
  - Adds a network constraint: `NetworkType.CONNECTED`.
  - Submits the work to `WorkManager`.
  - Observes the `WorkInfo` for that work ID and:
    - Reads `success` and `message` from the worker’s output data.
    - Shows an appropriate Toast to the user.

As a result, **each time the user opens the app**, the full app list is pushed to the API without any extra interaction.

---

## 7. Tech stack

- **Language:** Java (no Kotlin)
- **UI:** XML layout, single activity (`MainActivity`)
- **Architecture / components:**
  - `WorkManager` for background sync
  - `BroadcastReceiver` for new app installs
  - `SharedPreferences` for local storage / de-duplication
- **Networking:**
  - `Retrofit` for API client
  - `OkHttp` (with logging)
  - `Gson` for JSON serialization/deserialization
- **Android APIs:**
  - `PackageManager` to read installed apps
  - `Settings.Secure.ANDROID_ID` for `device_id`

---

## 8. Permissions explanation

The app declares the following key permissions:

- `INTERNET`  
  Required to send HTTP requests to the API endpoint.

- `QUERY_ALL_PACKAGES`  
  Needed to read the full list of installed applications on Android 11+ devices.  
  This is essential because the assignment specifically asks to:

  - Fetch all installed apps, and  
  - Send that complete list to the backend.

Without `QUERY_ALL_PACKAGES`, the app would not be able to see all installed packages on newer Android versions.

*(If targeting Play Store, this permission would require a strong justification in the Play Console; in this assignment context it is acceptable and required.)*

---

## 9. Steps to run the project

1. **Clone or download** this repository.
2. Open it in **Android Studio** (Giraffe or newer recommended).
3. Let Gradle sync and download dependencies.
4. Connect an Android device or start an emulator (Android 11+ is preferred for realistic behavior).
5. Click **Run** to install the app on the device.

No additional manual configuration is needed; the base URL and endpoint are hard-coded to the assignment API.

---

## 10. Testing steps

To verify all required behaviors:

1. **Full list sync on app open**
   - Install the app on a device.
   - Open the app.
   - Observe a Toast after a short delay:
     - Success: `"App list synced successfully"`.
     - Error: server-provided message or `"Failed to sync app list"`.
   - In Logcat (filter by `OkHttp`), confirm a `POST https://api.digitarmedia.com/test/applist` with `sync_type = "full_list"`.

2. **Full list sync via button**
   - With the app open, tap the **“Share App List”** button.
   - The same full-list flow should run again.
   - Confirm new logs and a Toast.

3. **New install detection (`ACTION_PACKAGE_ADDED`)**
   - Keep the app installed (foreground or background).
   - Install a new app from Play Store (any small test app).
   - In Logcat:
     - Look for `SendNewInstallWorker`.
     - Confirm a `POST` request with `sync_type = "new_install"` and only one app in the `apps` array.
   - If you try to reinstall or trigger the same package again, it should **not** send duplicates due to `SharedPreferences` tracking.

---

## 11. Notes about API responses

During development, the following behaviors were observed:

- **Success response (current behavior)**  
  The API replies with HTTP **200 OK** and a JSON body similar to:

  ```json
  {
    "success": true,
    "message": "Apps processed successfully",
    "inserted_or_updated": 234
  }
  ```

  In this case, the app:
  - Treats the call as successful.
  - Shows the Toast: **“App list synced successfully”**.

- **Error responses (example from earlier tests)**  
  At some point the API responded with HTTP **405 Method Not Allowed** and a body like:

  ```json
  {
    "success": false,
    "message": "Only POST allowed"
  }
  ```

  The app parses this JSON and shows the `"message"` content directly to the user in a Toast (e.g., `"Only POST allowed"`).  
  For non-2xx responses without a clear message, the app falls back to a generic error text.

The implementation is designed so that both success and error messages from the server are visible to the tester, which helps debug API-side issues.

---

## 12. Screenshots

_Add your own screenshots here when submitting the assignment, for example:_

- MainActivity with the **“Share App List”** button.
- Logcat showing `POST https://api.digitarmedia.com/test/applist` with `sync_type = "full_list"`.
- Logcat showing a `new_install` payload.
- Example Toast messages for success and error.

---

## 13. Final summary

This project implements the assignment requirements exactly:

- Reads the **full installed app list** on the device.
- **Never displays** that list to the user.
- Automatically syncs on app launch and on button press.
- Detects new installs and reports only the newly installed app.
- Sends the specified JSON structure using `ANDROID_ID` as `device_id`.
- Uses the required Android components and libraries: Java, XML, Retrofit, OkHttp, Gson, WorkManager, BroadcastReceiver, and `QUERY_ALL_PACKAGES`.

The code is focused, minimal, and aimed purely at satisfying the assignment’s functional and technical checklist.
