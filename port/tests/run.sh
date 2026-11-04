#!/bin/bash
set -euo pipefail
PROJECT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$PROJECT"
source scripts/env.sh
bash port/build.sh
mkdir -p work/port-tests
javac -d work/port-tests port/tests/android/util/Log.java
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.TouchRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.EditRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.AiRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.PredictionRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.GeometryRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.JoinedRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.EditorContextRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.LocalEditorRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.PrivacyRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.StartupRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.CandidatesRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.OptionalModelRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.PrivateFieldRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.SessionRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.SplitBudgetRegression
java -cp "work/port-tests:port/build/cls:$ANDROID_JAR" gbport.test.CommitRefreshRegression
