# OpenVisionAPI Android Frontend

A native Android (Java) app that lets you pick a photo, send it to an image-analysis API (Pixicular / OpenVisionAPI) and read the result as a clean summary: detected objects with confidence scores, text found in the image (OCR), request ID, processing time and credits used.

> **Status:** demo / learning project. The API key is compiled into the APK, so do not ship a build containing a private key (see [Security notes](#security-notes)).

## Features

- Single-screen app: landing section, image workspace and results card in one scrollable page.
- Pick **PNG, JPG or WEBP** images up to **10 MB** with the system photo picker, with preview, file name and size.
- Client-side validation: unsupported type, oversized, unreadable or empty images are rejected with a clear message before any upload.
- Multipart upload to the configured endpoint with `Authorization: Bearer <key>` and the `detect-labels` + `detect-text` services.
- Readable results: objects (+ categories), OCR text, request ID, processing time, credits.
- Friendly errors for offline, timeout, invalid key (401/403), rate limit (429), server errors (5xx), non-JSON and empty responses.
- Loading state ("Processing…"), **Try again** after a failure, **Reset** to clear everything, stale results are never shown for a different image.

## Prerequisites

| Requirement | Notes |
|---|---|
| Android Studio | A current stable release that supports Android Gradle Plugin **9.4.1** |
| JDK | Gradle's daemon is pinned to **JDK 25** (`android/gradle/gradle-daemon-jvm.properties`). Install JDK 25 and select it under *Settings → Build Tools → Gradle → Gradle JDK*. |
| Android SDK | Platform **36** (compile/target SDK); runs on **Android 7.0 (API 24)** and newer |
| Device | Emulator or physical phone with USB debugging |
| API access | Your own Pixicular API key (never commit it) |
| Internet | Needed for Gradle sync and for calling the API |

## Installation

```bash
git clone <repository-url>
cd OpenVisionAPI-frontend-android-app/android
cp local.properties.example local.properties     # Windows: copy local.properties.example local.properties
```

Edit `android/local.properties` (see [Configuration](#configuration)), then either:

**Android Studio** – *File → Open…* and choose the **`android`** folder (the one containing `settings.gradle`) → wait for Gradle sync → pick a device → **Run ▶**.

**Command line**

```bash
cd android
chmod +x gradlew            # only needed on macOS/Linux if the exec bit was lost
./gradlew assembleDebug     # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # installs on the connected device/emulator
```

## Configuration

Settings are read at **build time** and compiled into `BuildConfig`. Each one is looked up in this order: `android/local.properties` → environment variable of the same name → default.

| Name | Required | Default | Description |
|---|---|---|---|
| `IMAGE_API_URL` | No | `https://api.pixicular.com/v1/detect` | Full URL of the upload endpoint. **Use HTTPS** – Android blocks cleartext HTTP. |
| `PIXICULAR_API_KEY` | **Yes** | *(empty)* | Your API key, sent as a Bearer token. Without it the app shows "API key is not configured". |
| `sdk.dir` | Yes (local builds) | – | Path to the Android SDK. Android Studio writes it for you. Only in `local.properties`. |

`android/local.properties` is git-ignored. A template with **placeholders only** is provided in [`android/local.properties.example`](android/local.properties.example).

Using environment variables instead (CI, no file on disk):

```bash
export PIXICULAR_API_KEY="<your key>"
export IMAGE_API_URL="https://api.pixicular.com/v1/detect"
cd android && ./gradlew assembleDebug
```

After changing a value, rebuild (*Build → Rebuild Project*) – the old value stays inside the previously built APK.

## Usage guide

1. **Launch** the app. The landing section explains what it does.
2. Tap **Choose an image** (top section or in the workspace). Pick a PNG, JPG or WEBP file ≤ 10 MB. The page scrolls to the workspace where you see the preview, file name and size. A wrong type or a file over 10 MB shows an error and keeps **Process image** disabled.
3. Tap **Process image**. The button shows **Processing…** and the status line says *Sending image to OpenVisionAPI…*.
4. Read the result card: *Detected objects*, *Extracted text (OCR)*, request ID, processing time and credits. The text is selectable (long-press to copy).
5. If something fails, the status line explains why (offline, timeout, invalid key, …) and the button becomes **Try again**.
6. **Reset** clears the image, status and result. Choosing a different image also clears the previous result.

Screenshots: see [`docs/screenshots/`](docs/screenshots/README.md) (capture checklist and file names; images are added from a real device/emulator run).

## Running the tests

```bash
cd android
./gradlew testDebugUnitTest            # JVM unit tests (no device needed) – 50 tests
./gradlew connectedDebugAndroidTest    # UI smoke tests – needs an emulator/device
```

Unit tests cover the validator, the multipart API client (against a local HTTP server: success, 4xx/5xx, timeout, refused connection, malformed bodies, header-injection in file names), the response formatter and the error-message mapping. Reports: `android/app/build/reports/tests/testDebugUnitTest/index.html`.

QA documentation (test plan, manual test results, bug reports, final summary): [`docs/qa/`](docs/qa/).

## Project structure

```
android/app/src/main/java/com/openvisionapi/app/
  MainActivity.java          UI (built in code), picker, state handling
  OpenVisionApiClient.java   multipart POST over HttpURLConnection
  ApiResponseFormatter.java  JSON → readable text
  ApiErrorMessages.java      exceptions / HTTP codes → user messages
  ImageFileValidator.java    MIME type and size rules
android/app/src/test/…       JVM unit tests
android/app/src/androidTest/…  Espresso smoke tests
docs/qa/                     QA documents
.github/workflows/           CI (unit tests + debug build)
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| Gradle: *cannot find a Java installation matching the Daemon JVM requirements* | Install JDK 25 (see prerequisites). |
| *SDK location not found* | Add `sdk.dir=…` to `android/local.properties`. |
| App says *API key is not configured* | Set `PIXICULAR_API_KEY`, then rebuild. |
| HTTP 401/403 | The key is wrong, expired or has no access. |
| HTTP 404 | Check `IMAGE_API_URL`. |
| HTTP 429 | Rate limit or no credits left. |
| *Choose a PNG, JPG, or WEBP image* | The picked file is another type (GIF, HEIC, PDF…). Some cloud providers do not report a type; download the file first. |
| `./gradlew: Permission denied` | `chmod +x android/gradlew` |

## Security notes

- Keys live only in `android/local.properties` or environment variables – both outside version control. Never put a real key in the README, issues or screenshots.
- The key is embedded in `BuildConfig` and can be extracted from the APK. For anything beyond a demo, call your own backend that holds the key.
- The app requests only the `INTERNET` permission and reads images through the system picker.
