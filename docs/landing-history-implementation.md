# Landing Page and Analysis History

## Implemented behavior

- A persistent System / Light / Dark appearance selector is available from Landing, Image Analysis, and History. `ThemeManager` stores the choice in app-private `SharedPreferences`; a fresh install follows System. Semantic color resources have light and night variants, and system-bar icon contrast follows the active mode. No dependency was added. The redesigned screens use restrained 4–8 dp corner radii.

- `LandingActivity` is the launcher screen and provides links to the existing `MainActivity` analysis screen and `HistoryActivity`.
- The original image selection, request, response parsing, mock mode, and result rendering remain in `MainActivity`.
- A History button is available in the analysis header.
- `HistoryStore` uses application-private `SharedPreferences` for the small ordered index/metadata and application-private files for compressed JPEG thumbnails. No database dependency was added.
- Each record stores a unique ID, timestamp, image filename, thumbnail path, raw API/mock response, status, mock indicator, error message, short summary, and detected label count. Credentials are never stored in history.
- The thumbnail is derived from the submitted image and compressed; the full original image is not duplicated.
- Successful responses and failed attempts are recorded. Mock results are marked as mock data. Saving history is asynchronous and a history-storage failure does not hide the analysis result.
- Tapping a history record opens `MainActivity` in saved-result mode and parses the saved response locally. This path does not call the API. If a thumbnail has been deleted or cannot be decoded, the saved result remains accessible.
- Individual deletion and clear-all are supported; clear-all requires confirmation. History is sorted newest first and shows an empty state when there are no records.

## Files added

- `android/app/src/main/java/com/openvisionapi/app/LandingActivity.java`
- `android/app/src/main/java/com/openvisionapi/app/ThemeManager.java`
- `android/app/src/main/java/com/openvisionapi/app/HistoryActivity.java`
- `android/app/src/main/java/com/openvisionapi/app/HistoryStore.java`

## Files changed

- `android/app/src/main/java/com/openvisionapi/app/MainActivity.java`: header navigation, result/failure persistence, saved-result restoration.
- `android/app/src/main/AndroidManifest.xml`: launcher switched to `LandingActivity`; registered internal activities.
- `README.md`: documented landing, history, and appearance features.
- `android/app/src/main/res/values/colors.xml`, `values-night/colors.xml`, and `values/styles.xml`: semantic light/dark palettes and DayNight theme.
- `docs/theme-redesign.md`: design and implementation notes.
- `docs/tests.md`: appearance verification checklist.

## Persistence and privacy notes

History is local to the app installation and is removed if app data is cleared/uninstalled. API keys and auth tokens are not stored. The response JSON is stored locally so prior results can be shown without a network call. It may contain text extracted from images; users should delete records containing sensitive content.

## Verification status

Build and automated tests were attempted from `android/`. The wrapper needed line-ending normalization to execute, then could not download Gradle 9.6.0 because `services.gradle.org` did not resolve in this environment. Therefore compilation, unit tests, APK creation, emulator checks, and manual UI checks are **not verified** here. Run `gradlew.bat testDebugUnitTest assembleDebug` in Windows PowerShell or `./gradlew testDebugUnitTest assembleDebug` from a network-enabled environment with JDK 17+ and Android SDK configured.
