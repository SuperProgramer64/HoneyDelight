#!/usr/bin/env bash
set -euo pipefail
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${APKTOOL_JAR:?Set APKTOOL_JAR to apktool 2.12.1}"
[ "$#" -eq 1 ] && [ -f "$1" ] || { echo "Usage: $0 /path/to/original/base.apk" >&2; exit 1; }
python3 - "$ROOT/patches/compatibility.json" "$1" <<'CHECK'
import hashlib,json,sys
from pathlib import Path
expected=json.loads(Path(sys.argv[1]).read_text())['samsung_apk_sha256']
actual=hashlib.sha256(Path(sys.argv[2]).read_bytes()).hexdigest()
if actual!=expected:raise SystemExit('Unsupported Samsung APK: SHA-256 does not match the tested base.')
CHECK
[ ! -e "$ROOT/work/samsung" ] || { echo 'work/samsung already exists; use a fresh checkout or move it aside.' >&2; exit 1; }
mkdir -p "$ROOT/work"
java -jar "$APKTOOL_JAR" d -r "$1" -o "$ROOT/work/samsung"
python3 "$ROOT/scripts/apply-hooks.py" "$ROOT/work/samsung"
echo 'Samsung sources prepared locally. Proprietary files remain excluded from Git.'
