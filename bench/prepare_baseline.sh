#!/usr/bin/env bash
# Prepare the pre-optimization baseline in .baseline/ and compile both
# implementations plus the standalone Java benchmark harness.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_COMMIT="${1:-9bb6715}"
cd "$ROOT"

if [ ! -d .baseline ]; then
  git worktree add .baseline "$BASE_COMMIT"
fi
cp local.properties .baseline/local.properties 2>/dev/null || true

# JVM runs have no libandas_native.so; let the baseline fall back to Kotlin
# instead of aborting during object initialization.
python3 - <<'PY'
from pathlib import Path
for name in ["NativeMath.kt", "NativeData.kt", "NativeBatch.kt"]:
    path = Path(".baseline/android-pandas/src/main/java/cn/ac/oac/libs/andas/core") / name
    text = path.read_text()
    if "JVM benchmark fallback" in text:
        continue
    text = text.replace(
        '    init {\n        System.loadLibrary("andas_native")\n    }',
        '    init {\n        try {\n            System.loadLibrary("andas_native")\n'
        '        } catch (t: Throwable) {\n'
        '            // JVM benchmark fallback: native library is only available on Android\n'
        '        }\n    }',
    )
    path.write_text(text)
PY

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_USER_HOME="$ROOT/.android-home"
export GRADLE_USER_HOME="$ROOT/.gradle-home"
export GRADLE_RO_DEP_CACHE="${GRADLE_RO_DEP_CACHE:-$HOME/.gradle/caches}"
mkdir -p "$ANDROID_USER_HOME"
GRADLE_BIN="${GRADLE_BIN:-$(ls "$GRADLE_USER_HOME"/wrapper/dists/gradle-8.13-bin/*/gradle-8.13/bin/gradle 2>/dev/null | head -1)}"
if [ -z "${GRADLE_BIN:-}" ]; then
  GRADLE_BIN="$(ls "$HOME"/.gradle/wrapper/dists/gradle-8.13-bin/*/gradle-8.13/bin/gradle | head -1)"
fi

(cd .baseline && "$GRADLE_BIN" :android-pandas:compileDebugKotlin --offline --no-daemon --console=plain)
"$GRADLE_BIN" :android-pandas:compileDebugKotlin --offline --no-daemon --console=plain

STDLIB="$(find "$GRADLE_USER_HOME" "$HOME/.gradle" -path '*kotlin-stdlib/2.0.21/*' -name 'kotlin-stdlib-2.0.21.jar' 2>/dev/null | head -1)"
if [ -z "$STDLIB" ]; then
  echo "kotlin-stdlib-2.0.21.jar not found" >&2
  exit 1
fi
echo "$STDLIB" > bench/stdlib.path

mkdir -p bench/classes/new bench/classes/baseline
javac -encoding UTF-8 -cp "android-pandas/build/tmp/kotlin-classes/debug:$STDLIB" \
  -d bench/classes/new bench/java/Bench.java
javac -encoding UTF-8 -cp ".baseline/android-pandas/build/tmp/kotlin-classes/debug:$STDLIB" \
  -d bench/classes/baseline bench/java/Bench.java

echo "baseline and optimized classes are ready"
