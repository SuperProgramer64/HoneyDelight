#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/policy-tests
javac --release 11 -d build/policy-tests src/io/honeydelight/manager/UpdatePolicy.java tests/UpdatePolicyTest.java
java -cp build/policy-tests UpdatePolicyTest
