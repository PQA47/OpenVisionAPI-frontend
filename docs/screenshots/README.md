# Screenshots

No screenshots are committed yet: they must come from a real device/emulator run of the final build (the QA session that produced this documentation had no Android runtime).

Capture these with **no real API key or personal photo visible**, save as PNG in this folder with the names below, then link them from the main README.

| File | What to capture |
|---|---|
| `01-landing.png` | App just launched (hero section) |
| `02-workspace-empty.png` | Workspace card, nothing selected (Process image disabled) |
| `03-image-selected.png` | Preview, file name/size, Process image enabled |
| `04-processing.png` | Button shows "Processing…", status "Sending image…" |
| `05-result.png` | Successful result card |
| `06-error-invalid-type.png` | GIF/PDF rejected message |
| `07-error-offline.png` | Airplane-mode error + "Try again" button |
| `08-small-screen.png` | 360×640 phone |
| `09-tablet-landscape.png` | Tablet or landscape |

Tips: `adb exec-out screencap -p > 01-landing.png`, or Android Studio *Running Devices → Screenshot*. Use a public-domain test photo.

Link them in the README like this once added:

```markdown
![Landing](docs/screenshots/01-landing.png)
```
