# Custom-TV (TvLauncher)

A custom **Android TV home launcher** with a frosted-glass look, animated background, clock and date, a Favorites row, and a quick Settings button. Fully D-pad (remote) driven.

## Features
- Lists all installed Leanback and regular launcher apps, sorted A-Z
- **Favorites** row: long-press any app to pin or unpin it
- Focus animations (scale, glass highlight, label reveal) with smooth centered scrolling
- Clock, date, and a **Settings** shortcut in the header
- On-screen crash report on next launch, to debug on real TV hardware
- Back never closes the home screen
- No third-party dependencies

## Install
1. Open the **Actions** tab, pick the latest **Build APK** run, and download `TvLauncher-debug-apk`.
   (Tagged releases `v*` also publish the APK on the **Releases** page.)
2. Sideload `app-debug.apk` onto your Android TV / Google TV (for example with `adb install app-debug.apk`).
3. Press **Home** and choose **My Launcher** as the default home app.

## Build locally
Requires JDK 17, the Android SDK (platform 34) and Gradle 8.7.

```
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Controls
| Action | Remote |
|---|---|
| Move focus | D-pad |
| Launch app / press button | OK |
| Pin or unpin favorite | Long-press OK |
| Retry after a crash screen | OK |

## Contributing
See [CLAUDE.md](CLAUDE.md) for the code layout, conventions and verification checklist (works for humans too).
