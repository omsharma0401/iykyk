#!/usr/bin/env bash
# Runs one video through the real on-device pipeline (debug build) and prints the summary line.
#   tools/eval/run_on_device.sh path/to/video.mp4 [name]
# Requires: a connected device with the debug APK installed, adb on PATH.
# The video is copied through `run-as` so the app can read it (a plain `adb push` into the app's
# external dir is unreadable by the app under scoped storage). Also pulls observations.jsonl and a
# screenshot into tools/eval/out/<name>/ for analyze.py.
set -euo pipefail
VIDEO="$1"; NAME="${2:-$(basename "${VIDEO%.*}")}"
PKG=com.omsharma.iykyk
OUT="$(dirname "$0")/out/$NAME"; mkdir -p "$OUT"

adb shell "run-as $PKG sh -c 'mkdir -p files/test_videos'"
adb shell "run-as $PKG sh -c 'cat > files/test_videos/$NAME.mp4'" < "$VIDEO"
REMOTE="file:///data/user/0/$PKG/files/test_videos/$NAME.mp4"

adb shell am force-stop $PKG; adb logcat -c
adb shell am start -a $PKG.DEBUG_PROCESS_VIDEO -d "$REMOTE" -n $PKG/.MainActivity >/dev/null
for _ in $(seq 1 120); do
  sleep 2
  if adb logcat -d -s Pipeline:D | grep -q SUMMARY; then break; fi
done
adb logcat -d -s Pipeline:D Pipeline:E | sed 's/^.*Pipeline: //' | tee "$OUT/summary.txt"
sleep 2; adb exec-out screencap -p > "$OUT/screen.png"
adb shell "run-as $PKG sh -c 'cat files/observations.jsonl'" > "$OUT/observations.jsonl"
echo "wrote $OUT/{summary.txt,screen.png,observations.jsonl}"
