# Test record

## Automated tests added or maintained

- Unit tests cover MIME type and 10 MB size boundaries, API response parsing (labels, confidence, categories, OCR, metadata, bounding boxes, empty and malformed responses), and MOCK/DEMO gating/execution. The new `AnalysisFlowTest` verifies that mock success, empty, and error scenarios never invoke the live-request callback, while live mode still requires a valid image before the UI can submit.
- Instrumented tests cover package identity, parsing the mock fixtures, and UI execution of success, empty, and simulated-error/retry MOCK/DEMO scenarios without selecting an image.

## Commands actually run

- From `android/`: `./gradlew testDebugUnitTest assembleDebug` — **not run to completion**. The Gradle wrapper exited before Gradle started because neither `JAVA_HOME` nor a `java` executable is available (`ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.`). No unit test or APK build result is available.
- Instrumented tests — **not run**. No Android build could be produced and no emulator/device execution was available.
- Manual UI, image-picker, physical-device, and live API checks — **not run**. No emulator or physical Android device was available; live API checks also require user-provided credentials and endpoint.

Earlier pass entries in this file were not backed by run logs in the current environment and have been replaced with this observed test record.
