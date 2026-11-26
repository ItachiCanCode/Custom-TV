# Custom-TV

A custom Android TV launcher built with Kotlin. It provides a frosted-glass home screen for discovering, opening, and pinning TV and mobile apps.

## Features

- Android TV launcher with HOME and Leanback support
- Favorites and all-app rows
- Installed-app discovery for both TV and standard Android apps
- Long-press pinning for quick access
- D-pad and remote-friendly focus navigation
- Horizontal row scrolling that follows focused apps
- Frosted-glass cards and animated background effects
- On-screen crash report for startup failures

## Requirements

- Android device or emulator running Android 7.0 (API 24) or newer
- Android Studio with JDK 17
- Android SDK 34
- Gradle 8.7

## Build

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/ItachiCanCode/Custom-TV.git
cd Custom-TV
```

Build a debug APK with Gradle:

```bash
gradle assembleDebug --no-daemon
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

With an Android device connected through ADB:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

After installation, open the launcher and choose it as your device's default Home app when prompted.

## Controls

- Use the D-pad to move between apps.
- Press OK/Enter to launch the focused app.
- Long-press an app to add or remove it from Favorites.
- Press Back to keep the launcher open.

## Automation

The GitHub Actions workflow at [`.github/workflows/build.yml`](.github/workflows/build.yml) builds the debug APK on pushes and manual runs. The generated APK is uploaded as `TvLauncher-debug-apk`.

## Project Structure

```text
.
├── app/src/main/
│   ├── java/com/example/tvlauncher/MainActivity.kt
│   ├── res/
│   └── AndroidManifest.xml
├── .github/workflows/build.yml
├── build.gradle.kts
└── settings.gradle.kts
```

## License

No license has been specified for this repository.
