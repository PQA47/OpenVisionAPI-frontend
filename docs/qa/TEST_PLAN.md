# Test Plan & Checklist – OpenVisionAPI Android Frontend

## 1. Scope and mapping of the brief to this app

The brief was written generically (landing / upload / results pages, browsers). This project is a **native single-activity Android app**, so:

| Brief term | In this app |
|---|---|
| Landing page | Hero, features and workflow sections at the top of `MainActivity` |
| Upload interface | "Your visual workspace" card (preview, file info, Choose / Reset / Process image) |
| Results interface | Result card inserted into the workspace card after a request |
| Navigation | Scrolling, the header **About** button, and the hero/workspace **Choose an image** buttons (no separate screens) |
| Target browsers | Target **Android versions / devices / screen sizes** (section 3) |
| Console errors / failed requests | **Logcat** and HTTP traffic (Android Studio Network Inspector or a proxy) |
| Form reset | **Reset** button and automatic clearing when the selection changes |

Out of scope: the Pixicular API's own accuracy, backend behaviour, store distribution.

## 2. Requirements checklist (derived from code + README)

- [ ] R1 Pick PNG/JPG/WEBP only; max 10 MB; show preview + name + size
- [ ] R2 Process button disabled without a valid image and while a request runs
- [ ] R3 POST multipart (`file`, `services=detect-labels,detect-text`) with Bearer key to `IMAGE_API_URL`
- [ ] R4 Show labels (+categories, confidence), OCR text, request ID, time, credits
- [ ] R5 Clear errors for every failure class; never crash on odd responses
- [ ] R6 Reset restores the initial state; no stale result is ever shown
- [ ] R7 Key/endpoint come from `local.properties`/env; no secrets in repo
- [ ] R8 Works on API 24+ and typical phone/tablet sizes, portrait and landscape

## 3. Test environments

| Axis | Values |
|---|---|
| Android version | 7.0 (API 24, minSdk), 10 (29), 14 (34), 16 (36, targetSdk) |
| Screens | 360×640 dp (small), 411×891 dp (typical), 600×960 dp (7" tablet), 840×1280 dp (10" tablet), landscape |
| Font scale | 1.0, 1.3, 2.0 |
| Theme | System light and dark (app uses its own dark palette) |
| Network | Wi-Fi, airplane mode, throttled/slow, mock server |
| Mock API | Any local server (e.g. `python3 -m http.server`-style stub or the JUnit server) reachable at `http://10.0.2.2:PORT` – **cleartext HTTP is blocked on Android 9+; use an HTTPS tunnel or a debug network-security-config for manual runs** |

## 4. Critical workflows

| ID | Workflow |
|---|---|
| W1 | Launch → Choose image → preview → Process → read results (happy path) |
| W2 | Choose invalid / oversized / unreadable file → error → choose valid file → success |
| W3 | Process → failure (offline / timeout / 401 / 5xx / malformed) → Try again → success |
| W4 | Process → Reset (or pick another image) → no stale result, no late overwrite |
| W5 | Rotate / background the app during each step (known limitation OPEN-001) |

## 5. Test cases

Type: **A** = automated (JVM unit test), **M** = manual on device, **I** = inspection of code. Detailed outcomes are in `MANUAL_TEST_RESULTS.md`.

### Navigation
| ID | Steps | Expected | Type |
|---|---|---|---|
| NAV-01 | Launch app | Hero section visible, no crash, Process image disabled | M, A* |
| NAV-02 | Tap hero **Choose an image** | System picker opens | M |
| NAV-03 | Pick a valid image from the hero button | Page scrolls to workspace; preview/name/size visible | M |
| NAV-04 | Tap **About** | Scrolls to closing section | M |
| NAV-05 | Finish a request | Page scrolls to the result card | M |
| NAV-06 | Press Back | App closes (single screen) | M |

### Valid upload
| ID | Steps | Expected | Type |
|---|---|---|---|
| UPL-01/02/03 | Pick PNG / JPEG / WEBP | Preview shown, name + size, Process enabled | M, A (type check) |
| UPL-04 | Process valid image | Request: POST, Bearer key, multipart file+services | A |
| UPL-05 | API returns full JSON | Objects, OCR, request ID, time, credits rendered | A |
| UPL-06 | Image of exactly 10 MB / 10 MB + 1 byte | Accepted / rejected | A (limit), M |
| UPL-07 | Image with non-ASCII or quote/CRLF in file name | Request still valid, no header injection | A |

### Validation
| ID | Steps | Expected | Type |
|---|---|---|---|
| VAL-01 | Pick GIF / HEIC / SVG | "Choose a PNG, JPG, or WEBP image." Process disabled | A (type), M |
| VAL-02 | Pick non-image via a file provider | Same as VAL-01 | M |
| VAL-03 | Pick > 10 MB | "Choose an image smaller than 10 MB." | I, M |
| VAL-04 | Pick corrupt / 0-byte file named `.png` | "We could not open that image…" | I, M |
| VAL-05 | Tap Process with nothing selected | Button disabled; client also rejects null/empty | A, M* |
| VAL-06 | Open picker then cancel | State unchanged | I, M |
| VAL-07 | Build without key / endpoint | Clear "not configured" message | A |

### Network and API failures
| ID | Condition | Expected | Type |
|---|---|---|---|
| NET-01 | Airplane mode / DNS failure / refused | "Could not reach the server…", Try again | A (mapping, refused), M |
| NET-02 | Server slower than read timeout | Timeout message, no hang | A, M |
| NET-03 | HTTP 400/401/403/404/413/429/500/502 | Status-specific message; body shown if any | A |
| NET-04 | 200 + malformed JSON / HTML / plain text / `[]` | Raw (truncated) body shown, no crash | A |
| NET-05 | 200 + `success:false` | Treated as failure, not "completed" | A |
| NET-06 | 204 / empty body | "No results" text, no crash | A |
| NET-07 | JSON `null` / wrong types in fields | No "null" text, no crash | A |
| NET-08 | TLS failure | Secure-connection message | A (mapping) |

### UX: loading, errors, retry, reset
| ID | Steps | Expected | Type |
|---|---|---|---|
| UX-01 | Tap Process | Button disabled "Processing…", status text; no double request on double-tap | I, M |
| UX-02 | Any failure | Message in status line (announced via live region) | M |
| UX-03 | After failure | Button reads **Try again** and works | M |
| UX-04 | Tap Reset | Preview, name, status, result cleared; Process disabled | M |
| UX-05 | Reset while request in flight | Late response ignored | I, M |

### Repeated submissions / stale data
| ID | Steps | Expected | Type |
|---|---|---|---|
| REP-01 | Process A → Reset | Result removed | I, M |
| REP-02 | Process A → pick B | A's result removed; B starts clean | I, M |
| REP-03 | Process same image twice | One result card, replaced | I, M |
| REP-04 | Pick several images quickly | Only the last selection is previewed | I, M |
| REP-05 | Process A, pick B while uploading, process B | A's response never overwrites B's state | I, M |

### Responsive / compatibility
| ID | Check | Type |
|---|---|---|
| RSP-01 | 360×640: no clipped text, buttons reachable, nothing horizontally scrolls | M |
| RSP-02 | Font scale 2.0: headings wrap, 48 dp buttons not clipped | M |
| RSP-03 | Tablet / landscape: layout readable (content is not width-capped – expected wide) | M |
| RSP-04 | Light and dark system theme | M |
| RSP-05 | Insets / gesture navigation (edge-to-edge) | M |
| CMP-01 | Only APIs available at minSdk 24 are used | I |

### Security / configuration / logs
| ID | Check | Type |
|---|---|---|
| SEC-01 | No real key or token in repository | I (grep) |
| SEC-02 | `local.properties` ignored by git | I |
| SEC-03 | Key extractable from APK | Known risk OPEN-002 |
| CFG-01 | Env var fallback works | M |
| CFG-02 | Fresh clone → README steps → app runs | M |
| LOG-01 | Logcat clean of `AndroidRuntime`/`StrictMode` errors during W1–W4 | M |
| LOG-02 | Network inspector: exactly one POST per tap; correct headers | M |

\* A smoke test exists (`MainActivitySmokeTest`) but needs an emulator.

## 6. Commands

```bash
cd android
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
adb logcat -v time *:W | grep -E "openvisionapi|AndroidRuntime|StrictMode"
```

## 7. Entry / exit criteria

Entry: build compiles, `local.properties` configured. Exit (Definition of Done): W1–W4 pass, validation and failure cases verified, no open Critical/Major bug (or documented), configured automated tests pass, README steps verified on a clean machine, final regression done.
