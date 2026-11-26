# Custom-TV (TvLauncher)

![Build APK](https://github.com/ItachiCanCode/Custom-TV/actions/workflows/build.yml/badge.svg)

A custom **Android TV home launcher** with a frosted-glass look, animated background, clock and date, a Favorites row, and quick Wi-Fi and Settings buttons. Fully D-pad (remote) driven, with no third-party dependencies.

## Features
- Shows all installed Leanback and regular launcher apps in a wrapping A-Z grid, with names always visible
- **Favorites** row: long-press any app to pin or unpin it
- Focus animations (scale, glass highlight, label reveal) with smooth centered scrolling
- Clock, date, and **Wi-Fi** and **Settings** shortcuts in the header
- On-screen crash report on the next launch, for debugging on real TV hardware
- Back never closes the home screen

## Requirements
- Android TV / Google TV device or emulator running **Android 7.0 (API 24)** or newer
- A way to sideload an APK (for example `adb` or a file-manager app)

## Install
1. Open the **Actions** tab, pick the latest **Build APK** run, and download the `TvLauncher-debug-apk` artifact.
   Tagged releases (`v*`) publish the launcher APK and the setup apps on the **Releases** page.
2. Unzip if needed, then sideload `app-debug.apk` onto your TV, for example:
   ```
   adb connect <tv-ip-address>
   adb install app-debug.apk
   ```
3. Press **Home** and choose **My Launcher** as the default home app.

## Easy setup apps (no command line)
Each release includes two helpers that install the launcher on your TV for you:
- **`TvSetup-android.apk`**: install on your Android phone, enter the TV's IP address, tap **Install launcher on TV**.
- **`TvLauncherSetup.exe`**: Windows app. Enter the TV's IP address, click **Install launcher on TV** (it fetches `adb` automatically). You can also pick a local APK.

Before using either: on the TV open Settings > Device Preferences > About, click **Build** 7 times, then in Developer options enable **Network/USB debugging**. Keep the phone or PC on the same Wi-Fi as the TV and accept the debugging prompt shown on the TV. TVs that only offer "Wireless debugging" with a pairing code are not supported yet.

## Controls
| Action | Remote |
|---|---|
| Move focus | D-pad |
| Launch app / press a button | OK |
| Pin or unpin a favorite | Long-press OK |
| Retry after a crash screen | OK |

## Build locally
Requires JDK 17, the Android SDK (platform 34), and Gradle 8.7. There is no Gradle wrapper in the repo.

```
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Project structure
```
app/src/main/
  AndroidManifest.xml                          HOME + LEANBACK_LAUNCHER intent filter
  java/com/example/tvlauncher/MainActivity.kt  UI, app list, favorites, GlassDrawable
  res/                                         theme and TV banner
.github/workflows/
  build.yml                                    builds the debug APK on every push and PR
  release.yml                                  publishes the APK when a v* tag is pushed
```

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
