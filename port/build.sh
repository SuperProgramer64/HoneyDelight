#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source ../scripts/env.sh
rm -rf build
mkdir -p build/cls build/mod
sources=()
while IFS= read -r -d '' f; do sources+=("$f"); done < <(find stubs src harness cstubs -name '*.java' -print0)
javac --release 11 -encoding UTF-8 -cp "$ANDROID_JAR" -d build/cls "${sources[@]}"
classes=()
while IFS= read -r -d '' f; do classes+=("$f"); done < <(find build/cls/gbport build/cls/com/google -name '*.class' -print0)
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --output build "${classes[@]}"
mkdir -p build/modcls
cp -R build/cls/gbport build/cls/com build/modcls/
rm -rf build/modcls/gbport/test build/modcls/com/microsoft
classes=()
while IFS= read -r -d '' f; do classes+=("$f"); done < <(find build/modcls -name '*.class' -print0)
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --classpath build/cls --output build/mod "${classes[@]}"
