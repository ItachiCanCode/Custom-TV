# CLAUDE.md

Guidance for Claude (and other AI assistants) working in this repository.

## What this is

**TvLauncher** is a custom Android TV home-screen launcher. It lists installed apps as tiles, supports a pinned **Favorites** row, and uses a frosted-glass visual style with animated background blobs and a clock. It is driven entirely by the D-pad (TV remote).

- Package / applicationId: `com.example.tvlauncher`
- Single module: `:app` (root project name `TvLauncher`)
- Language: Kotlin, plain Android `Activity` and `android.widget` views. No Compose, no AndroidX, no XML layouts.

## Toolchain (match CI)

| Thing | Version |
|---|---|
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| Gradle | 8.7 |
| JDK | 17 (Temurin) |
| compileSdk / targetSdk | 34 |
| minSdk | 24 |

There is **no Gradle wrapper** checked in. CI installs Gradle 8.7 via `gradle/actions/setup-gradle`. Locally, use Android Studio or a system Gradle 8.7.

## Build

```
gradle assembleDebug --no-daemon
# output: app/build/outputs/apk/debug/app-debug.apk
```

CI (`.github/workflows/build.yml`, "Build APK") runs this on every push and on manual dispatch, and uploads the APK as the `TvLauncher-debug-apk` artifact. **A green CI run is the main check that a change compiles.** There are no unit or instrumentation tests yet.

## Code layout

```
app/src/main/
  AndroidManifest.xml                      # HOME + LEANBACK_LAUNCHER intent filter, <queries> for package visibility
  java/com/example/tvlauncher/MainActivity.kt   # all app logic + GlassDrawable
  res/values/styles.xml                    # Theme.Launcher (transparent, shows wallpaper)
  res/drawable/banner.xml                  # TV banner (solid blue rectangle)
```

### `MainActivity.kt`
- `onCreate` installs an uncaught-exception handler that saves the stack trace to SharedPreferences (`launcher` / `crash`). On next launch, `showCrash()` displays it on screen. Pressing OK/Enter clears it and calls `recreate()`.
- `buildUi()` creates the root `FrameLayout` (gradient background plus three animated "blob" views) and the vertical `content` container.
- `loadApps()` queries `CATEGORY_LEANBACK_LAUNCHER` and `CATEGORY_LAUNCHER`, excludes this app, de-duplicates by package, and sorts by label.
- `render()` rebuilds the whole screen: header (title, Wi-Fi and Settings buttons via `headerButton()`, date and time `TextClock`s), Favorites section (or a hint), and All Apps section.
- `tile()` builds one app tile: banner or icon in a glass card, a label shown only on focus, focus scale/elevation animation, click to launch, long-press to toggle favorite.
- `centerInRow()` animates horizontal scrolling itself. `SmoothScroll` disables the framework's focus-jump scrolling.
- `onKeyDown` swallows Back so the home screen never closes.
- `GlassDrawable` is a custom `Drawable` (translucent fill, top sheen, gradient edge stroke). Its `intensity` property is animated on focus.

### Persistence
SharedPreferences file `launcher`:
- `favs`: `Set<String>` of pinned package names
- `crash`: last uncaught stack trace (transient)

## Conventions and gotchas

- Keep it dependency-free (framework APIs only) unless there is a strong reason; the build files declare no libraries.
- UI is built in code. Sizes are expressed in dp and multiplied by the `dp` density field. Follow that pattern rather than adding XML layouts.
- Focus and D-pad navigation are the primary input. Anything new must be reachable and visibly focused with the remote. Touch is secondary.
- Watch Kotlin literal pitfalls with ARGB hex colors: values above `0x7FFFFFFF` overflow `Int` and need `.toInt()` (this broke the build once).
- The Back key is intentionally consumed. Do not remove this without providing another way to avoid leaving the launcher in a bad state.
- Keep the crash-report path working; it is the main debugging tool on real TV hardware where no logcat is handy.
- Package visibility on Android 11+ depends on the `<queries>` block in the manifest. If app listing breaks, check it first.

## Verifying changes

1. Push to a branch and confirm the **Build APK** workflow is green.
2. Download the `TvLauncher-debug-apk` artifact and sideload it onto an Android TV / Google TV device or emulator.
3. Set it as the default home app and check: tiles render, D-pad focus moves and animates, an app launches, long-press pins and unpins, the clock shows, and Back does nothing.

## Making changes

- Prefer small, focused commits on a branch, then open a PR to `main`.
- Update this file when the toolchain, layout, or conventions change.
