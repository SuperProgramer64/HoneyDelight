#!/usr/bin/env bash
# Uses only explicitly supplied base models; does not open apps, read user files or install a keyboard.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
source scripts/env.sh
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL for the selected test device}"
: "${BASE_COMPONENTS_DIR:?Supply the three pinned base components locally}"
A(){ adb -s "$ANDROID_SERIAL" "$@"; }
A get-state >/dev/null
python3 - "$BASE_COMPONENTS_DIR" <<'CHECK'
from pathlib import Path
import hashlib,json,sys
for name,digest in json.loads(Path('patches/base-components.json').read_text()).items():
    file=Path(sys.argv[1])/name
    if not file.is_file() or hashlib.sha256(file.read_bytes()).hexdigest()!=digest:
        raise SystemExit('Missing/unexpected fixture component: '+name)
CHECK
bash port/build.sh
TEST=work/native-predictions
rm -rf "$TEST"
mkdir -p "$TEST/classes" "$TEST/dex"
sources=()
while IFS= read -r -d '' file; do sources+=("$file"); done < <(find port/tests/native -name '*.java' -print0)
javac --release 11 -cp "port/build/cls:$ANDROID_JAR" -d "$TEST/classes" "${sources[@]}"
classes=()
while IFS= read -r -d '' file; do classes+=("$file"); done < <(find "$TEST/classes" port/build/cls/Qi -name '*.class' -print0)
while IFS= read -r -d '' file; do classes+=("$file"); done < <(find port/build/cls/com/microsoft -name '*.class' ! -name Prediction.class ! -name Predictions.class -print0)
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --classpath port/build/cls --output "$TEST/dex" "${classes[@]}"
REMOTE=/data/local/tmp/honeydelight-native-predictions
A shell mkdir -p "$REMOTE/data" "$REMOTE/crash"
for name in libintegrated_shared_object.so es_us.dict en_us.dict; do A push "$BASE_COMPONENTS_DIR/$name" "$REMOTE/$name" >/dev/null; done
A push "$TEST/dex/classes.dex" "$REMOTE/containers.dex" >/dev/null
A push port/build/classes.dex "$REMOTE/bridge.dex" >/dev/null
A shell "CLASSPATH=$REMOTE/containers.dex:$REMOTE/bridge.dex app_process /system/bin gbport.NativePredictionRegression $REMOTE"
