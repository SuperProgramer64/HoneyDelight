#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
source scripts/env.sh
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL for your rooted target device}"
: "${APKTOOL_JAR:?Set APKTOOL_JAR to apktool 2.12.1}"
: "${BASE_COMPONENTS_DIR:?Provide the three pinned Gboard base components locally}"
[ "$#" -eq 1 ] && [ -f "$1" ] || { echo "Usage: $0 /path/to/original/base.apk" >&2; exit 1; }
ORIGINAL_APK=$(python3 -c 'import pathlib,sys;print(pathlib.Path(sys.argv[1]).resolve())' "$1")
A(){ adb -s "$ANDROID_SERIAL" "$@"; }
A get-state >/dev/null
[ "$(A shell getprop ro.build.version.sdk | tr -d '\r')" = 36 ] || { echo 'Only SDK 36 is prepared' >&2; exit 1; }
[ "$(A shell getprop ro.product.cpu.abi | tr -d '\r')" = arm64-v8a ] || { echo 'Only arm64-v8a is prepared' >&2; exit 1; }
python3 - "$BASE_COMPONENTS_DIR" <<'CHECK'
import hashlib,json,sys
from pathlib import Path
pins=json.loads(Path('patches/base-components.json').read_text())
for name,expected in pins.items():
 file=Path(sys.argv[1])/name
 if not file.is_file() or file.is_symlink() or hashlib.sha256(file.read_bytes()).hexdigest()!=expected:
  raise SystemExit('Missing or unexpected base component: '+name)
CHECK
APK=$(A shell pm path --user 0 com.samsung.android.honeyboard | sed -n 's/^package://p' | head -n 1 | tr -d '\r')
[ -n "$APK" ] || { echo 'Samsung Keyboard not found' >&2; exit 1; }
EXPECTED=$(python3 -c 'import json;print(json.load(open("patches/compatibility.json"))["samsung_apk_sha256"])')
ACTUAL=$(A shell su -c "sha256sum '$APK'" | cut -d' ' -f1 | tr -d '\r')
[ "$ACTUAL" = "$EXPECTED" ] || { echo 'Target Samsung APK hash does not match' >&2; exit 1; }
bash scripts/prepare-samsung.sh "$ORIGINAL_APK"
bash port/build.sh
java -jar "$APKTOOL_JAR" b work/samsung -o work/patched-local.apk
mkdir -p work/device-payload/in work/device-payload/merge dist/local-payload
unzip -p work/patched-local.apk classes.dex > work/device-payload/in/classes.dex
unzip -p work/patched-local.apk classes2.dex > work/device-payload/in/classes2.dex
unzip -p "$ORIGINAL_APK" classes3.dex > work/device-payload/stock-classes3.dex
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --output work/device-payload/merge work/device-payload/stock-classes3.dex port/build/mod/classes.dex
[ "$(find work/device-payload/merge -name '*.dex' | wc -l | tr -d ' ')" = 1 ] || { echo 'Merged bridge exceeded one DEX' >&2; exit 1; }
cp work/device-payload/merge/classes.dex work/device-payload/in/classes3.dex
python3 - <<'ZIP'
from pathlib import Path
import zipfile
with zipfile.ZipFile('work/device-payload/mod.zip','w',zipfile.ZIP_STORED) as z:
 for p in sorted(Path('work/device-payload/in').glob('*.dex')):z.write(p,p.name)
ZIP
cp work/device-payload/mod.zip dist/local-payload/mod.zip
bash module-tools/build.sh
for name in libintegrated_shared_object.so es_us.dict en_us.dict; do cp "$BASE_COMPONENTS_DIR/$name" "dist/local-payload/$name"; done
A shell getprop ro.build.fingerprint | tr -d '\r' > dist/fingerprint.txt
python3 scripts/build-module.py --payload-dir dist/local-payload --fingerprint-file dist/fingerprint.txt --output-dir dist
printf 'Built a compatible module; ART is prepared locally during module installation. No keyboard installation was changed.\n'
