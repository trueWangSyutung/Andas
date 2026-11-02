#!/usr/bin/env bash
# 在已连接的真机上通过 adb 直接安装并运行完整 Android 实验（CSV/一致性/JNI/数值）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/android-pandas/build/outputs/apk/androidTest/debug/android-pandas-debug-androidTest.apk"
RUNNER="cn.ac.oac.libs.andas.test/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="${TEST_CLASS:-}"

SERIAL="${ANDROID_SERIAL:-}"
if [[ -z "$SERIAL" ]]; then
  SERIAL="$(adb devices | awk '$2 == "device" && $1 !~ /^emulator/ {print $1; exit}')"
fi
if [[ -z "$SERIAL" ]]; then
  echo "未发现真机。请连接手机并开启 USB 调试，或用 ANDROID_SERIAL=<serial> 指定。" >&2
  exit 2
fi

QEMU="$(adb -s "$SERIAL" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')"
if [[ "$QEMU" == "1" ]]; then
  echo "拒绝在模拟器 $SERIAL 上运行；请连接真机。" >&2
  exit 3
fi

MODEL="$(adb -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
ANDROID="$(adb -s "$SERIAL" shell getprop ro.build.version.release | tr -d '\r')"
API="$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"

echo "device=$SERIAL model=$MODEL android=$ANDROID api=$API"

if [[ "${REBUILD:-1}" == "1" || ! -f "$APK" ]]; then
  export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
  export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$ROOT/.android-home}"
  export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT/.gradle-home}"
  "$ROOT/gradlew" :android-pandas:assembleDebugAndroidTest --no-daemon --console=plain ${GRADLE_ARGS:-}
fi

adb -s "$SERIAL" install -r -t "$APK" >/dev/null
adb -s "$SERIAL" logcat -c

set +e
if [[ -n "$TEST_CLASS" ]]; then
  adb -s "$SERIAL" shell am instrument -w -r -e class "$TEST_CLASS" "$RUNNER" \
    | tee "$ROOT/bench/results/android_adb_instrument.txt"
else
  adb -s "$SERIAL" shell am instrument -w -r "$RUNNER" \
    | tee "$ROOT/bench/results/android_adb_instrument.txt"
fi
STATUS=${PIPESTATUS[0]}
set -e

LOG="$ROOT/bench/results/android_${MODEL// /_}_adb.log"
adb -s "$SERIAL" logcat -d > "$LOG"
adb -s "$SERIAL" logcat -d -s ANDAS_BENCH:I > "$ROOT/bench/results/android_${MODEL// /_}_bench.log"
adb -s "$SERIAL" logcat -d -s AndasNumpyBench:I > "$ROOT/bench/results/android_${MODEL// /_}_numpy.log"
python3 "$ROOT/bench/parse_android_bench.py" "$ROOT/bench/results/android_${MODEL// /_}_bench.log" "$ROOT/bench/results/android_physical.csv"
cp "$ROOT/bench/results/android_adb_instrument.txt" "$ROOT/bench/results/android_adb_full_instrument.txt"
echo "raw log: $LOG"
echo "csv: $ROOT/bench/results/android_physical.csv"
exit "$STATUS"
