#!/usr/bin/env bash
# Runs against a booted TV emulator: installs the launcher, opens it, presses remote keys,
# opens the app menu, saves screenshots, and fails if the app crashed.
# Failure reasons are emitted as GitHub annotations so they show up without opening logs.
# Usage: scripts/smoke-test.sh <api-level>
set -u
API="${1:-x}"
PKG=com.example.tvlauncher
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=smoke-out
mkdir -p "$OUT"

annotate() {  # level, message
  local m="$2"; m="${m//'%'/'%25'}"; m="${m//$'\r'/'%0D'}"; m="${m//$'\n'/'%0A'}"
  echo "::$1 title=API $API smoke::$m"
}
shot() { adb exec-out screencap -p > "$OUT/api$API-$1.png" 2>/dev/null || true; }
key()  { adb shell input keyevent "$1"; sleep 1; }

INSTALL_OUT=$(adb install -r "$APK" 2>&1) || { annotate error "install failed: $INSTALL_OUT"; exit 1; }
adb logcat -c
START_OUT=$(adb shell am start -n "$PKG/.MainActivity" 2>&1)
sleep 12
shot 1-home
for k in KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_RIGHT KEYCODE_DPAD_RIGHT KEYCODE_DPAD_LEFT KEYCODE_DPAD_UP; do key $k; done
shot 2-after-navigation
key KEYCODE_DPAD_DOWN
key KEYCODE_MENU
sleep 2
shot 3-app-menu
key KEYCODE_BACK            # closes the dialog
key KEYCODE_BACK            # Back must not close the launcher
sleep 2
shot 4-after-back
adb logcat -d > "$OUT/api$API-logcat.txt"

# Only crashes of OUR process count (the emulator's own system apps may crash on their own).
if grep -q "Process: $PKG," "$OUT/api$API-logcat.txt"; then
  annotate error "launcher crash detected:
$(grep -B1 -A30 "Process: $PKG," "$OUT/api$API-logcat.txt" | head -45)"
  exit 1
fi
if ! adb shell "ps -A 2>/dev/null || ps" | grep -q "$PKG"; then
  annotate error "launcher process not running after start. am start said: $START_OUT
logcat tail:
$(grep -i "tvlauncher\|AndroidRuntime\|ActivityManager" "$OUT/api$API-logcat.txt" | tail -25)"
  exit 1
fi
annotate notice "PASS"
echo "PASS: API $API"
