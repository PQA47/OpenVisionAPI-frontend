# Test Results

**Build under test:** working tree after the QA changes (see `BUG_REPORTS.md`) · **Date:** 2026-10-27
**Environment actually available:** Linux container, OpenJDK 21, JUnit 4.13.2. **No Android SDK, no emulator/device, no access to Maven Central / Google Maven / Gradle services**, so the app could not be built, installed or run, and `./gradlew` could not be executed.

Legend

| Mark | Meaning |
|---|---|
| ✅ PASS | Executed here (JVM test) and passed |
| 🔎 INSPECTED | Code path read and judged correct; **not executed** |
| ⏳ PENDING | Needs a device/emulator; **not yet run** – the tester fills in the last column |
| ❌ FAIL | Found a defect (see BUG id) |

The "Device result" column is intentionally empty: do not mark a case done without running it.

## A. Automated tests (executed)

`javac` + JUnit 4.13.2 on JDK 21; `org.json` supplied by a **local stand-in** (the real `org.json:json:20240303` could not be downloaded) – re-run with Gradle to confirm.

| Class | Tests | Result |
|---|---|---|
| `ImageFileValidatorTest` | 6 | ✅ |
| `ApiErrorMessagesTest` | 6 | ✅ |
| `ApiResponseFormatterTest` | 17 | ✅ |
| `OpenVisionApiClientTest` (real `HttpURLConnection` ↔ local `HttpServer`) | 21 | ✅ |
| **Total** | **50** | **✅ 50 / 50** |

Test sensitivity: 6 deliberate code mutations (size limit off-by-one, unsanitised file name, missing `Bearer`, success default `true`, read timeout removed, error stream ignored) were each caught by at least one test.

Before the fixes: unit-test compilation failed (BUG-001).

## B. Case results

| ID | Result | Evidence | Device result |
|---|---|---|---|
| NAV-01 | ⏳ | `MainActivitySmokeTest.landingPageShowsHeroContent` written, not run | |
| NAV-02 | 🔎 | `imagePicker.launch("image/*")` on both buttons | |
| NAV-03 | 🔎 | `scrollToView(uploadCard)` on selection (BUG-008) | |
| NAV-04 | 🔎 | About → `smoothScrollTo` last section | |
| NAV-05 | 🔎 | `scrollToView(result)` after render | |
| NAV-06 | ⏳ | – | |
| UPL-01/02/03 | ✅ type rule · ⏳ UI | `acceptsSupportedImageTypes` | |
| UPL-04 | ✅ | `requestIsAuthenticatedMultipartPost`, `apiKeyIsTrimmed` | |
| UPL-05 | ✅ | `formatsFullSuccessResponse` (golden output) | |
| UPL-06 | ✅ limit · ⏳ UI | `validatesSizeBoundaries`, `largestAllowedImageCanBeUploaded` (10 MB upload) | |
| UPL-07 | ✅ | `filenameCannotInjectHeaders`, `missingFilenameFallsBackToDefault` | |
| VAL-01 | ✅ rule · ⏳ UI | `rejectsUnsupportedOrMissingTypes` (gif, heic, svg, pdf, null…) | |
| VAL-02 | ⏳ | – | |
| VAL-03 | 🔎 | `readImageBytes` throws `ImageTooLargeException` above 10 MB | |
| VAL-04 | 🔎 | `decodeImage` → IOException → `image_read_error` | |
| VAL-05 | ✅ client · ⏳ UI | `missingImageIsRejected`; button disabled logic in `updateSubmitButton` | |
| VAL-06 | 🔎 | `loadSelectedImage(null)` returns without change | |
| VAL-07 | ✅ | `missingEndpointIsRejectedBeforeAnyNetworkCall`, `missingApiKeyIsRejected` | |
| NET-01 | ✅ client/mapping · ⏳ UI | `closedPortCausesConnectionFailure`, `offlineErrorsDoNotLeakHostnameOnly` | |
| NET-02 | ✅ | `slowServerCausesReadTimeout` (fails in < 1.4 s with 300 ms timeout) | |
| NET-03 | ✅ | 401, 429, 500, 502 client tests + `httpStatusMessages` | |
| NET-04 | ✅ | malformed JSON, plain text, `[1,2,3]`, long HTML | |
| NET-05 | ✅ | `successFlagIsReadFromJsonObjectOnly` | |
| NET-06 | ✅ | `emptySuccessBodyIsReturnedAsEmptyString`, `emptyOrNullBodyShowsNoResults` | |
| NET-07 | ✅ | `jsonNullValues…`, `wrongTypedFieldsDoNotCrash`, `nonObjectLabelEntriesAreSkipped` | |
| NET-08 | ✅ mapping | `tlsFailureHasOwnMessage` | |
| UX-01 | 🔎 | `processing` guard + disabled button; **text-only loading indicator** (OPEN-005) | |
| UX-02 | 🔎 | status line has polite live region | |
| UX-03 | 🔎 | `lastRequestFailed` → "Try again" | |
| UX-04 | ⏳ | `MainActivitySmokeTest.resetWithoutSelectionKeepsInitialState` (not run); code inspected | |
| UX-05 | 🔎 | `requestGeneration` check | |
| REP-01…05 | 🔎 | `invalidateRequestAndResult`, `imageLoadGeneration`, separate executors | |
| RSP-01…05 | ⏳ | Layout is `match_parent` + `ScrollView`, sizes in dp/sp – no fixed widths seen; needs visual check | |
| CMP-01 | 🔎 | APIs used (`setLetterSpacing` 21, `setAccessibilityLiveRegion` 19, `isDestroyed` 17, try-with-resources) are ≤ API 24 | |
| SEC-01 | ✅ | regex scan for key/secret/token assignments: only the placeholder `PASTE_YOUR_API_KEY_HERE` | |
| SEC-02 | ✅ | `android/.gitignore` lists `local.properties` | |
| SEC-03 | known | OPEN-002 | |
| CFG-01 | ⏳ | Groovy `System.getenv` fallback added, not built | |
| CFG-02 | ⏳ | README written from the code; **not** verified on a clean machine (see OPEN-007) | |
| LOG-01/02 | ⏳ | no Logcat / traffic capture possible here | |

## C. Console / network review

- **Logcat / console errors:** not reviewable (app never ran). The app contains no logging, so failures leave no trace (OPEN-006).
- **Failed network requests:** exercised only against the local test server. Observed client behaviour: non-2xx responses keep status and body; timeouts and refused connections surface as `SocketTimeoutException` / `ConnectException`; no retries or crashes.

## D. Responsive and "browser" matrix to execute

| Device / config | Portrait | Landscape | Font 1.3 | Font 2.0 | Dark |
|---|---|---|---|---|---|
| 360×640, API 24 | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ |
| 411×891, API 34 | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ |
| 7" tablet, API 29 | ⏳ | ⏳ | ⏳ | – | ⏳ |
| 10" tablet, API 36 | ⏳ | ⏳ | – | – | ⏳ |

Browsers are not applicable to this native app.
