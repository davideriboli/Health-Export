# HealthExport

Android app that reads data from **Android Health Connect** and exports it to a **Google Spreadsheet** — one tab per data type.

Built for personal use and distributed via APK sideload.

---

## Features

- Wizard UI (4 steps): select data types → time range → destination → confirm
- Exports all Health Connect record types (steps, heart rate, sleep, weight, nutrition, and more)
- Google Sheets destination: one tab per record type, fixed column schema + `source_app` column
- Overwrite or append mode
- Scheduled automatic exports (daily / weekly / monthly) via WorkManager
- Preferences persisted across sessions (DataStore)
- Material 3 UI with warm palette

## Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Health data | Android Health Connect (`androidx.health.connect`) |
| Google Sheets | Sheets API v4 via `google-api-client-android` |
| Google auth | Credential Manager |
| Preferences | Jetpack DataStore |
| Scheduling | WorkManager |
| DI | Hilt |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Build

The project builds entirely via **GitHub Actions** — no Android Studio required.

Every push to `main` triggers a build and produces a signed APK as a workflow artifact.

### GitHub Secrets required

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Release keystore encoded in base64 (`base64 -w0 mykeystore.jks`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

### Local build (optional)

```bash
# Debug APK (no signing required)
./gradlew assembleDebug

# Release APK (requires the four env vars above)
KEYSTORE_FILE=/path/to/release.jks \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
./gradlew assembleRelease
```

Requires JDK 17+ and Android SDK (or let the CI do it for you).

---

## Install

Download the APK from the latest [Actions run](../../actions) → artifact `HealthExport-release-<sha>`, then sideload it:

```bash
adb install app-release.apk
```

Or transfer the file to the device and open it with a file manager (enable *Install unknown apps* for that app in Android settings).

---

## Development plan

| Module | Status | Description |
|---|---|---|
| 1 | ✅ Done | Project scaffold, Gradle, Material 3 theme, wizard navigation |
| 2 | ⏳ Next | Health Connect integration, data type selection UI |
| 3 | — | Google auth + Sheets write |
| 4 | — | Append / overwrite logic, dynamic tabs |
| 5 | — | WorkManager scheduled export |
| 6 | — | UI polish, error handling, edge cases |
