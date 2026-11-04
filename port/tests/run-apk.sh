#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
mkdir -p work/apk-model-tests
javac --release 11 -d work/apk-model-tests port/apk-src/gbport/ApkModels.java port/tests/apk/gbport/ApkModelRegression.java
java -cp work/apk-model-tests gbport.ApkModelRegression
