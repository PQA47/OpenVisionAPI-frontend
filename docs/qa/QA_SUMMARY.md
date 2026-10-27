# Final QA Summary

**Project:** OpenVisionAPI Android frontend · **Date:** 2026-10-27

## Verdict: **not yet ready to sign off** – code-level QA is done; device-level QA and the clean-machine setup check remain.

The application logic was tested thoroughly and a real defect set was fixed, but the app could not be built or run in the QA environment, so the end-to-end UI workflow and the Definition of Done items that need an Android runtime are still open.

## What was done

| Area | Outcome |
|---|---|
| Test plan / checklist | `TEST_PLAN.md` – requirements, workflows, about 50 cases, device matrix |
| Automated tests | 50 JVM unit tests (validator, HTTP client against a local server, response formatter, error mapping) – **50/50 pass**; 3 Espresso smoke tests written (not run). Placeholder `2+2` test removed. |
| Defects | 13 fixed (1 Critical, 3 Major, 4 Medium, 5 Low), 10 documented as open – `BUG_REPORTS.md` |
| Documentation | README rewritten (overview, features, prerequisites, install, configuration table, run, usage, tests, troubleshooting, security); `local.properties.example`; env-var fallback; screenshot checklist |
| CI | `.github/workflows/android-ci.yml` runs unit tests + debug build on every PR (not yet run) |

## Most important findings

1. **The existing unit tests could not compile** (BUG-001) – any CI would have been red from day one.
2. **Stale / late results** (BUG-002/003/004): a result for image A could appear next to image B, Reset did not cancel the request, and an upload blocked previews.
3. HTTP 200 with `success:false` was reported as "completed"; raw exception text such as "…: null" reached users; hero-button selection gave no visible feedback.
4. Still open and worth scheduling: state lost on rotation (OPEN-001), API key inside the APK (OPEN-002), and the JDK 25 requirement that likely breaks a clean setup (OPEN-007).

## Definition of Done – status

| Item | Status |
|---|---|
| Primary workflow tested end to end | ❌ Not done – needs device; request/response chain tested at JVM level |
| Critical validation and API-failure scenarios verified | ✅ logic · ⏳ on-screen messages |
| Major defects fixed or documented | ✅ (UI fixes inspected, not run – re-check on device) |
| Configured automated tests pass | ✅ 50/50 locally with a stand-in `org.json`; ⏳ confirm with `./gradlew testDebugUnitTest` |
| Setup instructions verified on a clean environment | ❌ Not done |
| Documentation matches final behaviour | ✅ matched against the code; screenshots missing |
| Final regression on integrated app | ❌ Not done – needs device |
| Changes merged through approved PRs | ❌ The supplied zip had no git history; open a PR from these changes |

## Risks and caveats

- `MainActivity.java` was edited but **not compiled** (no Android SDK). A syntax/symbol check showed only missing-Android-class errors; the new methods resolve. Treat the first Gradle build as the real compile check.
- `ApiResponseFormatterTest` ran against a local stand-in for `org.json`; `app/build.gradle` now depends on the real `org.json:json:20240303` for unit tests.
- API contract not verified against the provider.
- The CI workflow and the `System.getenv` fallback in `build.gradle` are unexecuted.

## Next steps (in order)

1. Open a PR with these changes; let CI run `testDebugUnitTest` and `assembleDebug`. Fix anything the real compiler reports.
2. On a device/emulator: run the ⏳ cases in `MANUAL_TEST_RESULTS.md` (W1–W4 first, using a slow/failing mock server), capture screenshots into `docs/screenshots/`, check Logcat.
3. Follow the README on a clean machine; settle OPEN-007 (JDK 25).
4. Triage OPEN-001 (rotation) and OPEN-002 (key handling); re-run the regression and update this summary.
