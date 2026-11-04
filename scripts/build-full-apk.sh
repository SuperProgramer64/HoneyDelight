#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
source scripts/env.sh
: "${FULL_APK_KEYSTORE:?Set FULL_APK_KEYSTORE outside the repository}"
: "${FULL_APK_STORE_PASS:?Set FULL_APK_STORE_PASS in the environment}"
[ "$#" -eq 1 ] && [ -f "$1" ] || { echo 'Usage: build-full-apk.sh /path/to/original/base.apk' >&2; exit 1; }
STOCK=$(python3 -c 'from pathlib import Path;import sys;print(Path(sys.argv[1]).resolve())' "$1")
BUILD="$ROOT/work/full-apk"
case "$STOCK" in "$BUILD"/*) echo 'Keep the input APK outside work/full-apk' >&2; exit 1;; esac
[ -f dist/local-payload/mod.zip ] || { echo 'Build the current module payload first' >&2; exit 1; }
rm -rf "$BUILD"
mkdir -p "$BUILD/port" "$BUILD/scripts" "$BUILD/merge"
cp scripts/env.sh "$BUILD/scripts/"
cp port/build.sh "$BUILD/port/"
for directory in src stubs cstubs harness; do cp -R "port/$directory" "$BUILD/port/$directory"; done
cp port/apk-src/gbport/*.java "$BUILD/port/src/gbport/"
python3 - "$BUILD/port/src/gbport/Bridge.java" <<'BOOTSTRAP'
from pathlib import Path
import sys
file=Path(sys.argv[1]); source=file.read_text()
needle='if (s != null) configureDirectory(s.getFilesDir());'
if source.count(needle)!=1:raise SystemExit('The bootstrap integration point changed; inspect before building')
file.write_text(source.replace(needle,needle+'\n            ApkBootstrap.prepare(s);'))
BOOTSTRAP
bash "$BUILD/port/build.sh"
unzip -p "$STOCK" classes3.dex > "$BUILD/stock-classes3.dex"
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --output "$BUILD/merge" "$BUILD/stock-classes3.dex" "$BUILD/port/build/mod/classes.dex"
[ "$(find "$BUILD/merge" -name '*.dex' | wc -l | tr -d ' ')" = 1 ] || { echo 'Merged APK bridge exceeded one DEX' >&2; exit 1; }
python3 scripts/build-full-apk.py --stock "$STOCK" --payload-dir dist/local-payload --classes3 "$BUILD/merge/classes.dex" --output "$BUILD/unsigned.apk"
BT="$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS_VERSION:-34.0.0}"
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
OUTPUT="$ROOT/dist/HoneyDelight-Keyboard-0.1.0-alpha-experimental.apk"
"$BT/apksigner" sign --ks "$FULL_APK_KEYSTORE" --ks-pass env:FULL_APK_STORE_PASS --out "$OUTPUT" "$BUILD/aligned.apk"
"$BT/apksigner" verify "$OUTPUT"
"$BT/zipalign" -c -p 4 "$OUTPUT"
printf 'Built experimental full APK: %s\nSamsung signature is not preserved; runtime installation remains unvalidated.\n' "$OUTPUT"
