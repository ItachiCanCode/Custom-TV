#!/usr/bin/env bash
# Runs against a booted TV emulator: installs the launcher, opens it, presses remote keys,
# opens the app menu, saves screenshots, and fails if the app crashed.
# Usage: scripts/smoke-test.sh <api-level>
set -u
API="${1:-x}"
PKG=com.example.tvlauncher
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=smoke-out
mkdir -p "$OUT"
shot() { adb exec-out screencap -p > "$OUT/api$API-$1.png" 2>/dev/null || true; }
key()  { adb shell input keyevent "$1"; sleep 1; }

adb install -r "$APK" || { echo "FAIL: install"; exit 1; }
adb logcat -c
adb shell am start -n "$PKG/.MainActivity"
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

if ! adb shell "ps -A 2>/dev/null || ps" | grep -q "$PKG"; then
  echo "FAIL: launcher process is not running"; exit 1
fi
if grep -q "FATAL EXCEPTION" "$OUT/api$API-logcat.txt"; then
  echo "FAIL: crash detected"; grep -A25 "FATAL EXCEPTION" "$OUT/api$API-logcat.txt" | head -60; exit 1
fi
echo "PASS: API $API"
