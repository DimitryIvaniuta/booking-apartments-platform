#!/usr/bin/env sh
# Minimal Gradle wrapper script (POSIX).
# Delegates to org.gradle.wrapper.GradleWrapperMain in gradle/wrapper/gradle-wrapper.jar.

APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

JAVA_CMD="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
