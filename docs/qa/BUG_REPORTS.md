# Bug Reports & Verification Notes

Severity: **Critical** (blocks build/tests or core flow) · **Major** (wrong behaviour users will hit) · **Medium** · **Low**.

Verification levels used below:

- **V-JVM** – reproduced before / verified after by running JVM tests (`javac` + JUnit 4.13.2, local HTTP server).
- **V-INSPECT** – verified by reading the code path only; UI code could not be compiled or run in the QA environment (no Android SDK, Maven/Google repositories blocked).
- Every `V-INSPECT` item has a **device re-check** step that the responsible member must execute before the item is closed.

Fixes are in the working tree of this change set. Merge them through a pull request and tick "device re-check" there.

---

## Fixed

### BUG-001 – Unit tests do not compile (Critical)
- **Steps:** `./gradlew testDebugUnitTest`
- **Expected:** tests run. **Actual:** compilation error, `cannot find symbol: method isValidSize(long)` in `ImageFileValidatorTest`; the whole unit-test task fails.
- **Cause:** test references a method that does not exist in `ImageFileValidator`. A placeholder `NativeAppUnitTest` (`2 + 2`) also gave false confidence.
- **Fix:** added `ImageFileValidator.isValidSize(long)` (1 … 10 MB inclusive); removed the placeholder; rewrote the test.
- **Verification:** V-JVM – 4 compile errors before, 50 passing tests after. ✅

### BUG-002 – Stale result remains after Reset or choosing another image (Major)
- **Steps:** Process image A → result shown → tap **Reset** (or choose image B).
- **Expected:** result card disappears. **Actual:** old result stays under the now-empty/new image.
- **Fix:** `invalidateRequestAndResult()` removes the result card on reset, on every new selection and on rejected files.
- **Verification:** V-INSPECT. Device re-check: REP-01, REP-02.

### BUG-003 – Late response of an abandoned request overwrites the UI; Reset allows duplicate uploads (Major)
- **Steps:** Process A; during the upload tap **Reset**, choose image B.
- **Expected:** A's response is discarded. **Actual:** `resetImage()` only set `processing=false`; when A finished it re-enabled the button and drew A's result next to B; the user could also start a second paid request while A was still running.
- **Fix:** `requestGeneration` token; each response is applied only if it belongs to the latest request and the activity is alive.
- **Verification:** V-INSPECT. Device re-check: UX-05, REP-05 (use a slow mock server).

### BUG-004 – Upload blocks image preview (Major)
- **Steps:** Process A (slow server) → choose B.
- **Expected:** B previews immediately. **Actual:** loading and uploading shared one single-thread executor, so B stayed on "Loading image preview…" until the upload finished (up to ~60 s).
- **Fix:** separate `uploadWorker`.
- **Verification:** V-INSPECT. Device re-check: REP-05.

### BUG-005 – "Processing completed" shown for failed analysis (Medium)
- **Steps:** API answers HTTP 200 with `{"success":false,…}`, an empty body, or non-JSON.
- **Expected:** failure message. **Actual:** status "Processing completed (HTTP 200)" above a "⚠ ANALYSIS FAILED" card.
- **Fix:** `ApiResponseFormatter.isAnalysisSuccessful()` decides the status text and enables retry.
- **Verification:** V-JVM (`successFlagIsReadFromJsonObjectOnly`) + V-INSPECT for the UI wiring. ✅/🔎

### BUG-006 – Raw exception text shown to users (Medium)
- **Steps:** airplane mode; very slow server; exception without message.
- **Expected:** readable message. **Actual:** "Could not process image: api.pixicular.com" (`UnknownHostException`), "…: Read timed out", "…: null".
- **Fix:** `ApiErrorMessages.forException()`.
- **Verification:** V-JVM (`ApiErrorMessagesTest`). ✅

### BUG-007 – Same generic message for every HTTP error; no retry affordance (Medium)
- **Actual:** "API error (HTTP 401). Please try again." for invalid key, rate limit and server errors alike; the button stayed "Process image".
- **Fix:** `ApiErrorMessages.forHttpStatus()` (key, endpoint, size, rate-limit, server) and the button reads **Try again** after any failure.
- **Verification:** V-JVM for messages; V-INSPECT for the button label. Device re-check: UX-03.

### BUG-008 – Choosing an image from the hero button gives no visible feedback (Medium)
- **Steps:** tap hero **Choose an image**, pick a photo.
- **Expected:** user sees the preview. **Actual:** picker closes on the hero section; the preview and Process button are several screens below.
- **Fix:** auto-scroll to the workspace after selecting, and to the result after processing.
- **Verification:** V-INSPECT. Device re-check: NAV-03, NAV-05.

### BUG-009 – OCR section prints a heading with no content (Low)
- **Steps:** response with detections that are all blank.
- **Fix:** shows "No text detected." **Verification:** V-JVM (`blankOcrDetectionsAreReportedAsNoText`). ✅

### BUG-010 – Stray spaces in OCR line: `( LINE · 99.10% confidence )` (Low)
- **Fix:** `(LINE · 99.10% confidence)`. **Verification:** V-JVM (full-output golden test). ✅

### BUG-011 – JSON `null` could render as the word "null" on Android (Low)
- **Cause:** Android's `org.json.optString()` returns `"null"` for JSON null (the reference library returns the default). Applies to `requestId`, label names, categories, OCR text/type, photo status.
- **Fix:** check `isNull()` first. **Verification:** V-JVM with the reference semantics (`jsonNullValuesAreNotPrintedAsTheWordNull`); the Android-specific behaviour itself was not run on a device.

### BUG-012 – Huge raw error bodies flood the screen (Low)
- **Steps:** gateway returns a long HTML 502 page. **Fix:** truncated to 1,500 characters. **Verification:** V-JVM. ✅

### BUG-013 – Image decode failure other than `IOException` leaves "Loading image preview…" forever (Low)
- **Fix:** catch `RuntimeException`/`OutOfMemoryError` → read-error message. **Verification:** V-INSPECT.

---

## Open / documented (not fixed in this change)

| ID | Sev. | Description | Reproduction | Expected | Recommendation / owner |
|---|---|---|---|---|---|
| OPEN-001 | Medium | Rotation, theme/locale change or process death loses the selected image, status and result; an in-flight response is dropped, so a paid call can be lost. | Select image → rotate device. | State survives configuration changes. | Save the picked `Uri` + result text in `onSaveInstanceState` and reload, or declare `configChanges`. Needs on-device testing, so not changed blind. |
| OPEN-002 | Medium (security) | API key is compiled into `BuildConfig`; recoverable from the APK. | `apktool`/`jadx` on a debug build. | Key not shipped to clients. | Backend proxy before any distribution. Documented in README. |
| OPEN-003 | Low | File type is taken from `ContentResolver.getType()`; providers returning `null`/`application/octet-stream` for real images are rejected. | Pick an image from a provider with missing MIME. | Accept by sniffing magic bytes. | Optional improvement. |
| OPEN-004 | Low | Many UI strings are hard-coded in `MainActivity` ("Process image", "Processing…", status texts), not in `strings.xml`. | – | Localisable. | Move to resources. |
| OPEN-005 | Low | No spinner/progress widget; progress is text-only. No cancel button; worst-case wait ≈ 15 s connect + upload + 45 s read. | Process on a slow network. | Visible progress and cancel. | UX decision. |
| OPEN-006 | Low | No logging at all, so failures leave no Logcat trace. | – | Debug-level logs without secrets. | Add `Log.w` in catch blocks. |
| OPEN-007 | Medium (setup) | `gradle-daemon-jvm.properties` demands JDK 25 and `settings.gradle` configures no toolchain resolver, so a machine without JDK 25 probably cannot start Gradle. **Not verified – could not run Gradle here.** | Clean machine with JDK 17/21 only. | Setup works with documented JDK. | Confirm; either keep JDK 25 (documented) or lower the criteria with the team. |
| OPEN-008 | Low | API contract (`file`, `services` fields, `Bearer` auth, response keys) is unverified against the provider docs (source comment says so). Client limit of 10 MB is not confirmed server-side. | – | Verified. | Check against Pixicular docs / one real call. |
| OPEN-009 | Low | Content is not width-capped on tablets/landscape (text lines become very long). Feature icons are decorative glyphs without content descriptions. | Open on 10" tablet. | Comfortable width; accessible labels. | Design follow-up. |
| OPEN-010 | Info | `android:allowBackup="true"`; Gradle files use several AGP compatibility flags (`android.newDsl=false`, …) that may need attention on future AGP upgrades. | – | – | Review at next upgrade. |

---

## Verification log / hand-off

| Item | Verified by | Result | Remaining action |
|---|---|---|---|
| BUG-001, 005 (logic), 006, 009, 010, 011 (logic), 012 | QA (JVM run, with mutation checks) | ✅ | none |
| BUG-002, 003, 004, 005 (UI), 007 (label), 008, 013 | QA (code inspection) | 🔎 | Developer to confirm on device; QA to tick REP/UX/NAV cases |
| OPEN-001 … 010 | – | documented | Owners to triage |

Template for new bugs:

```
ID / Title / Severity
Environment (device, Android version, build)
Steps to reproduce
Expected behaviour
Actual behaviour (+ Logcat / screenshot)
Fix PR · Verified by · Date
```
