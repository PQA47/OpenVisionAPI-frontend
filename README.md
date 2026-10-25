# OpenVisionAPI Android App

OpenVisionAPI is a native Android application built with Java and Android views.

## Requirements

- Android Studio
- Android SDK Platform 36
- JDK 17 (Android Studio's bundled JDK is recommended)

## Open and run

1. In Android Studio, choose **Open** and select this project's `android` folder.
2. Allow Gradle to sync and install any prompted Android SDK components.
3. Choose an Android emulator or connected device and run the `app` configuration.

Alternatively, from PowerShell:

```powershell
Set-Location 'C:\path\to\OpenVisionAPI-frontend\android'
.\gradlew.bat assembleDebug
```

The debug APK is generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Image preview

The native image picker accepts PNG, JPG, and WEBP images up to 10 MB. Selected images are previewed locally and are not uploaded. AI analysis is not connected; a backend endpoint and request/response contract are needed before it can be implemented.

## Android project

- `android/app/src/main/java/com/openvisionapi/app/MainActivity.java` — native app screen and image picker
- `android/app/src/main/res/layout/activity_main.xml` — Android screen layout
- `android/app/src/main/res/values/` — app theme, colors, and text
