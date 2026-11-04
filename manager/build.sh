#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source ../scripts/env.sh
: "${MANAGER_KEYSTORE:?Set MANAGER_KEYSTORE outside the repository}"
: "${MANAGER_STORE_PASS:?Set MANAGER_STORE_PASS in the environment}"
BT="$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS_VERSION:-34.0.0}"
rm -rf build
mkdir -p build/classes assets
cp ../assets/banner.png assets/banner.png
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$ANDROID_JAR" --manifest AndroidManifest.xml -A assets build/res.zip
sources=()
while IFS= read -r -d '' f; do sources+=("$f"); done < <(find src -name '*.java' -print0)
javac --release 11 -encoding UTF-8 -cp "$ANDROID_JAR" -d build/classes "${sources[@]}"
classes=()
while IFS= read -r -d '' f; do classes+=("$f"); done < <(find build/classes -name '*.class' -print0)
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --output build "${classes[@]}"
cp build/base.apk build/unsigned.apk
(cd build && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$MANAGER_KEYSTORE" --ks-pass env:MANAGER_STORE_PASS --out build/HoneyDelight-Manager-alpha.apk build/aligned.apk
"$BT/apksigner" verify build/HoneyDelight-Manager-alpha.apk
