#!/usr/bin/env bash
# Behavior test against a booted TV emulator. Installs the launcher exactly like the setup apps do,
# then drives it with remote keys and reads the real on-screen contents (uiautomator) to check:
# header, sections, app tiles, the app menu, pinning, persistence after restart, unpinning, Back, no crash.
# Failure reasons are emitted as GitHub annotations. Usage: scripts/smoke-test.sh <api-level>
set -u
API="${1:-x}"
PKG=io.github.itachicancode.voidlauncher
APK=app/build/outputs/apk/debug/VoidLauncher-debug.apk
OUT=smoke-out
HINT="Long-press any app to pin it here"
mkdir -p "$OUT"

annotate() {  # level, message
  local m="$2"; m="${m//'%'/'%25'}"; m="${m//$'\r'/'%0D'}"; m="${m//$'\n'/'%0A'}"
  echo "::$1 title=API $API smoke::$m"
}
fail() { annotate error "$1"; adb logcat -d > "$OUT/api$API-logcat.txt" 2>/dev/null; adb exec-out screencap -p > "$OUT/api$API-failure.png" 2>/dev/null; exit 1; }
shot() { adb exec-out screencap -p > "$OUT/api$API-$1.png" 2>/dev/null || true; }
key()  { adb shell input keyevent "$1"; sleep 1; }

dump() {  # read what is really on screen
  local i
  for i in 1 2 3; do
    adb shell uiautomator dump --compressed /sdcard/ui.xml > "$OUT/dump.log" 2>&1
    adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
    grep -q "<hierarchy" "$OUT/ui.xml" && return 0
    sleep 2
  done
  fail "could not read the screen: $(cat "$OUT/dump.log")"
}
has_text() { grep -q "text=\"$1\"" "$OUT/ui.xml"; }
tap_text() {
  local xy
  xy=$(python3 - "$1" "$OUT/ui.xml" << 'PY'
import re, sys
import xml.etree.ElementTree as ET
want, path = sys.argv[1], sys.argv[2]
for n in ET.parse(path).iter("node"):
    if n.get("text") == want:
        x1, y1, x2, y2 = map(int, re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds")).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  [ -n "$xy" ] || fail "could not find '$1' on screen"
  adb shell input tap $xy
  sleep 2
}

# 1) Install the way the setup apps do, and make it the home app
INSTALL_OUT=$(adb install -r "$APK" 2>&1) || fail "install failed: $INSTALL_OUT"
SETHOME=$(adb shell cmd package set-home-activity "$PKG/.MainActivity" 2>&1)
RESOLVED=$(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1)
if echo "$RESOLVED" | grep -q "$PKG"; then annotate notice "set-home-activity works: launcher resolves as the home app"
else annotate warning "launcher did not become the home app via set-home-activity: $SETHOME / $RESOLVED"; fi

# 2) Launch and check the screen content
adb logcat -c
START_OUT=$(adb shell am start -n "$PKG/.MainActivity" 2>&1)
sleep 12
shot 1-home
dump
has_text "Home"       || fail "header title 'Home' not on screen. am start: $START_OUT"
has_text "All Apps"   || fail "'All Apps' section not on screen"
has_text "Favorites"  || fail "'Favorites' section not on screen"
has_text "$HINT"      || fail "expected the empty-favorites hint on a fresh install"
TILES=$(grep -c 'content-desc="[^"]\+"' "$OUT/ui.xml")
[ "$TILES" -ge 1 ] || fail "no app tiles found on screen"
annotate notice "screen OK: $TILES app tiles visible"

# 3) Remote navigation
for k in KEYCODE_DPAD_DOWN KEYCODE_DPAD_RIGHT KEYCODE_DPAD_LEFT KEYCODE_DPAD_UP; do key $k; done
shot 2-after-navigation

# 4) App menu -> pin to Favorites
key KEYCODE_DPAD_DOWN
key KEYCODE_MENU
sleep 1; shot 3-app-menu; dump
has_text "Pin to Favorites" || fail "app menu did not open (no 'Pin to Favorites')"
has_text "App info"         || fail "app menu is missing 'App info'"
has_text "Uninstall"        || fail "app menu is missing 'Uninstall'"
tap_text "Pin to Favorites"
dump
has_text "$HINT" && fail "pinning had no effect: empty-favorites hint is still shown"
shot 4-pinned

# 5) Favorite survives a restart
adb shell am force-stop "$PKG"
adb shell am start -n "$PKG/.MainActivity" > /dev/null 2>&1
sleep 8; dump
has_text "$HINT" && fail "favorite was not saved across a restart"

# 6) Unpin
key KEYCODE_MENU
sleep 1; dump
has_text "Remove from Favorites" || fail "menu on a favorite does not offer 'Remove from Favorites'"
tap_text "Remove from Favorites"
dump
has_text "$HINT" || fail "unpinning did not restore the empty-favorites hint"
annotate notice "pin, persist and unpin all work"

# 7) Back must not leave the launcher
key KEYCODE_BACK; key KEYCODE_BACK
sleep 2; shot 5-after-back
adb logcat -d > "$OUT/api$API-logcat.txt"

if grep -q "Process: $PKG," "$OUT/api$API-logcat.txt"; then
  fail "launcher crash detected:
$(grep -B1 -A30 "Process: $PKG," "$OUT/api$API-logcat.txt" | head -45)"
fi
adb shell "ps -A 2>/dev/null || ps" | grep -q "$PKG" || fail "launcher process not running at the end"
annotate notice "PASS"
echo "PASS: API $API"
