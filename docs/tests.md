# Test record

## Automated tests present

- `ImageFileValidatorTest`: supported MIME types and size boundaries.
- `ApiResponseParserTest`: labels, confidence, categories, OCR, metadata, bounding boxes, empty and malformed responses.
- `AnalysisFlowTest`: mock success/empty/error scenarios and live-request gating.
- `HistoryStoreTest`: history entry serialization round-trip and safe defaults for incomplete records.
- No automated theme preference test is currently configured; verify theme switching and persistence using the manual checklist below.
- Instrumented tests cover package identity, mock response parsing, and the existing MOCK/DEMO UI scenarios.

## Verification performed in this environment

- Parsed all Android manifest/resource XML files with Python's XML parser: passed.
- Performed a lexical delimiter-balance check on Java source and test files: passed. This is a static sanity check, not compilation.
- Attempted `./gradlew testDebugUnitTest assembleDebug` from `android/`. The wrapper started after normalizing the Unix script's CRLF line endings, but it could not download Gradle 9.6.0 because `services.gradle.org` did not resolve.
- Therefore Java/Android compilation, unit tests, APK generation, instrumented tests, emulator/physical-device checks, and live API checks remain unverified in this environment.

## Manual test checklist (not yet executed)

1. App launch opens `LandingActivity` and follows System appearance on a fresh install.
2. On Landing, select Light, verify the screen, status bar, navigation bar, and buttons update immediately; navigate to Analysis and History and verify consistent colors.
3. Select Dark from Analysis or History and verify all existing screens update without restarting the app.
4. Select System, change the Android system appearance, and verify the app follows it.
5. Force-close and relaunch after selecting Light and Dark; confirm the explicit selection persists.
6. Analyze an Image opens the existing analysis screen.
7. History opens and shows a helpful empty state for a new install.
8. Existing image selection, preview, live request, and mock mode still work.
9. Successful mock/live results appear in History with timestamp, summary, status, and label count.
10. Opening a saved success item displays the stored response without a network request.
11. History persists after force-closing and relaunching the app.
12. Individual deletion removes the selected record and thumbnail.
13. Clear all requires confirmation and removes all records.
14. Failed requests and mock failures appear as failed records.
15. Missing thumbnail files do not prevent saved results from opening.
16. Back navigation returns to the previous screen.
17. Check button disabled states, spinner, errors, confirmation dialogs, text contrast, and 4–8 dp corner radii on a small screen.

Run on a network-enabled development machine with JDK 17+, Android SDK Platform 36, and a compatible Gradle distribution using `gradlew.bat testDebugUnitTest assembleDebug` on Windows or `./gradlew testDebugUnitTest assembleDebug` on macOS/Linux.
