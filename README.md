# OpenVisionAPI Android frontend (Pixicular)

## Setup API credentials

Create or edit `android/local.properties` (next to `android/settings.gradle`). Keep this file private and do not commit it.

```properties
sdk.dir=C:\Users\YOUR_WINDOWS_USER\AppData\Local\Android\Sdk
IMAGE_API_URL=https://api.pixicular.com/v1/detect
PIXICULAR_API_KEY=PASTE_YOUR_API_KEY_HERE
```

Keep your existing `sdk.dir` value if Android Studio already configured it. Replace only the API URL/key if your working Pixicular endpoint differs from the default above. Do not share the key in screenshots or chat.

## Android Studio

1. Extract this project folder.
2. Open the `android` folder in Android Studio (the folder containing `settings.gradle`).
3. Add the API properties to `android/local.properties`.
4. Sync Gradle, then Build > Rebuild Project.
5. Run the app, select an image, and tap Process image.

`MainActivity.java` displays a readable summary of detected labels, confidence, OCR text, request ID, processing time, and credits. `OpenVisionApiClient.java` sends the multipart request and returns the raw JSON response for the UI to format.

If your previously working Pixicular request used a different URL, keep that exact URL in `IMAGE_API_URL`. The API key is compiled into BuildConfig in this demo, so for a production app use a backend proxy rather than shipping a private key in the APK.
