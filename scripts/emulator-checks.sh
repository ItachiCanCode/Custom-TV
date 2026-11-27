#!/usr/bin/env bash
# Runs inside a booted TV emulator: (1) the phone setup app's install logic over ADB-TCP, (2) the behavior test.
# Usage: scripts/emulator-checks.sh <api-level>
set -u
API="${1:-x}"
annotate() { local m="$2"; m="${m//'%'/'%25'}"; m="${m//$'\r'/'%0D'}"; m="${m//$'\n'/'%0A'}"; echo "::$1 title=API $API setup-app::$m"; }

adb -s emulator-5554 tcpip 5555 > /dev/null 2>&1
adb wait-for-device; sleep 6
adb -s emulator-5554 forward tcp:15555 tcp:5555
APK_ABS="$PWD/app/build/outputs/apk/debug/VoidLauncher-debug.apk"
FAILED=0
if LOG=$(cd setup-android && TV_ADB_HOST=127.0.0.1 TV_ADB_PORT=15555 TV_APK="$APK_ABS" gradle testDebugUnitTest --no-daemon --console=plain 2>&1); then
  annotate notice "installer test PASS (download-free install + set-home over ADB)"
else
  annotate error "installer test failed:
$(echo "$LOG" | tail -40)"
  FAILED=1
fi
bash scripts/smoke-test.sh "$API" || exit 1
exit $FAILED
