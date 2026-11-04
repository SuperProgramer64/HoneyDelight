#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source ../scripts/env.sh
rm -rf build
mkdir -p build/classes
javac --release 11 -d build/classes src/io/honeydelight/tools/CrcPatch.java
java -cp "$D8_JAR" com.android.tools.r8.D8 --release --min-api 33 --lib "$ANDROID_JAR" --output build build/classes/io/honeydelight/tools/CrcPatch.class
cp build/classes.dex ../module/tools.dex
