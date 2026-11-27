# Void Launcher

![Build APK](https://github.com/ItachiCanCode/Custom-TV/actions/workflows/build.yml/badge.svg)

A custom **Android TV home launcher** with a frosted-glass look, animated background, clock and date, a Favorites row, and quick Wi-Fi and Settings buttons. Fully D-pad (remote) driven, with no third-party dependencies.

## Features
- Shows all installed Leanback and regular launcher apps in a wrapping A-Z grid, with names always visible
- Every app keeps its **original artwork**: its own TV banner, or its real icon on a backdrop tinted from the icon's colors
- **Favorites** row, plus a long-press menu on any app: pin/unpin, App info, Uninstall
- Focus animations (scale and glass highlight) with smooth scrolling
- Clock, date, and **Wi-Fi** and **Settings** shortcuts in the header
- On-screen crash report on the next launch, for debugging on real TV hardware
- Back never closes the home screen

## Requirements
- Android TV / Google TV device or emulator running **Android 5.0 (API 21)** or newer
- A way to sideload an APK (for example `adb` or a file-manager app)

## Install
1. Download `VoidLauncher.apk` from the latest [Release](https://github.com/ItachiCanCode/Custom-TV/releases/latest).
   (Or, for the newest unreleased build, download the `VoidLauncher-debug-apk` artifact from the latest **Build APK** run in the Actions tab.)
2. Sideload `VoidLauncher.apk` onto your TV, for example:
   ```
   adb connect <tv-ip-address>
   adb install VoidLauncher.apk
   ```
3. Press **Home** and choose **Void Launcher** as the default home app.

## Easy setup apps (no command line)
Each release includes two helpers that install the launcher on your TV for you:
- **`VoidLauncherSetup-Android.apk`**: install on your Android phone, enter the TV's IP address, tap **Install launcher on TV**.
- **`VoidLauncherSetup.exe`**: Windows app. Enter the TV's IP address, click **Install launcher on TV** (it fetches `adb` automatically). You can also pick a local APK.

Before using either: on the TV open Settings > Device Preferences > About, click **Build** 7 times, then in Developer options enable **Network/USB debugging**. Keep the phone or PC on the same Wi-Fi as the TV and accept the debugging prompt shown on the TV. TVs that only offer "Wireless debugging" with a pairing code are not supported yet.

## Controls
| Action | Remote |
|---|---|
| Move focus | D-pad |
| Launch app / press a button | OK |
| Open app menu (pin, info, uninstall) | Long-press OK, or the Menu / Info button |
| Retry after a crash screen | OK |

## Build locally
Requires JDK 17, the Android SDK (platform 34), and Gradle 8.7. There is no Gradle wrapper in the repo.

```
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/VoidLauncher-debug.apk`

## Project structure
```
app/                    Void Launcher (the TV app)
  src/main/AndroidManifest.xml                 HOME + LEANBACK_LAUNCHER intent filter
  src/main/java/io/github/itachicancode/voidlauncher/MainActivity.kt  UI, app list, favorites, GlassDrawable
setup-android/          phone app that installs the launcher on a TV over ADB
setup-windows/          Windows app that does the same (.NET WinForms)
.github/workflows/
  build.yml             builds the launcher APK on every push and PR
  build-setup.yml       builds the Android and Windows setup apps
  release.yml           on a v* tag, publishes the launcher and both setup apps
```

## Compatibility
- Built for Android TV and Google TV from Android 5.0 (API 21) through Android 14, at 720p, 1080p and 4K (all sizes are in dp, and the grid adapts to the screen width). The build fails if the code uses an API newer than Android 5.0 without a guard.
- A CI job launches the launcher on TV emulators for Android 9, 11 and 14, presses remote keys, opens the app menu and fails on any crash (see the **TV Compatibility** workflow). Android 5.0 to 8.x are not covered by the emulator test yet, so they are untested.
- Wi-Fi and Settings buttons only appear if the TV has those settings screens.
- Works with a remote (D-pad), a mouse, and a gamepad.
- **Limits:** some TVs do not let you change the default home app (for example Amazon Fire TV and some operator or OEM builds). There the launcher still installs and opens from the app list, but the Home button keeps opening the stock launcher. This can't be fixed from inside the app.
- Not verified on physical hardware yet. Please report your TV model and Android version in Issues.

## Troubleshooting
- **Red "Launcher error" screen:** photograph or copy the text, then press OK to retry. Include the text if you file a bug.
- **Launcher not offered when pressing Home:** some TVs restrict changing the default home app. Look under the TV's Settings for Apps or Default apps, or reboot after installing.
- **Want the original launcher back:** change the default home app in the same Settings area, or uninstall this app.
- **An app is missing:** only apps that declare a launcher or Leanback launcher activity are listed.

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md). For the code layout, conventions, and verification checklist, see [CLAUDE.md](CLAUDE.md), which works for humans and AI assistants alike.

## Status
**Beta.** Expect rough edges. Please report problems in [Issues](https://github.com/ItachiCanCode/Custom-TV/issues).

## License
MIT. See [LICENSE](LICENSE).
