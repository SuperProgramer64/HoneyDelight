#!/usr/bin/env bash
set -euo pipefail
ANDROID_HOME=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/share/android-commandlinetools}}
ANDROID_BUILD_TOOLS_VERSION=${ANDROID_BUILD_TOOLS_VERSION:-34.0.0}
ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
D8_JAR="$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/lib/d8.jar"
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
[ -f "$ANDROID_JAR" ] || { echo "Missing Android platform 34: $ANDROID_JAR" >&2; exit 1; }
[ -f "$D8_JAR" ] || { echo "Missing D8: $D8_JAR" >&2; exit 1; }
command -v javac >/dev/null || { echo 'JDK 17 required' >&2; exit 1; }
export ANDROID_HOME ANDROID_JAR D8_JAR
