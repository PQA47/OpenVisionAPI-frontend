# Appearance redesign

## Implementation

- Kept the existing Java + Android Views architecture; no Jetpack Compose migration and no new dependencies.
- Added `ThemeManager`, which stores `system`, `light`, or `dark` in app-private `SharedPreferences` and applies it with `AppCompatDelegate`.
- The default for a fresh install is `System`; explicit Light and Dark choices persist across restarts. The AppCompat DayNight theme follows system changes when System is selected.
- Added semantic color resources in `res/values/colors.xml` and matching dark values in `res/values-night/colors.xml`.
- Landing, Image Analysis/Results, and History resolve colors from the active resource configuration. Status and navigation bar icon contrast is adjusted for the active theme.
- Added a Theme entry point on each main screen. The selector offers System default, Light mode, and Dark mode.
- Reduced primary container and button corner radii to a restrained range (typically 4–8 dp), using spacing and dividers to create hierarchy.

## Functionality preserved by design

No changes were made to API endpoint/payload handling, image selection, response parsing, MOCK/DEMO scenarios, history storage schema, or saved-result navigation. This is a UI/theme change.

## Verification status

XML parsing and static Java delimiter checks should be run after edits. The Android Gradle build and automated tests must be run on a machine where the configured Gradle distribution can be downloaded or is already cached. A source-level check is not a substitute for a successful build or device test. See `tests.md` for the current verification record and manual test checklist.
