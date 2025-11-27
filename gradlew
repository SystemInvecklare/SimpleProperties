#!/usr/bin/env bash
set -e
set -o pipefail

# Get the directory of this script
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Read project.gradlerage
GRADLE_VERSION=""
JAVA_VERSION=""

while IFS='=' read -r key value; do
    case "$key" in
        gradle) GRADLE_VERSION="$value" ;;
        java)   JAVA_VERSION="$value" ;;
    esac
done < "$DIR/project.gradlerage"

if [[ -z "$GRADLE_VERSION" ]]; then
    echo "gradle version not set in project.gradlerage"
    exit 1
fi

if [[ -z "$JAVA_VERSION" ]]; then
    echo "java version not set in project.gradlerage"
    exit 1
fi

# Launch GradleRage.jar with versions as arguments
java -jar "$DIR/GradleRage.jar" "$GRADLE_VERSION" "$JAVA_VERSION"

# Read .gradlerage/paths.properties
JAVA_BIN=""
GRADLE_JAR=""

while IFS='=' read -r key value; do
    case "$key" in
        java_path) JAVA_BIN="$value" ;;
        gradle_launcher) GRADLE_JAR="$value" ;;
    esac
done < "$DIR/.gradlerage/paths.properties"

# Validate
if [[ -z "$JAVA_BIN" ]]; then
    echo "ERROR: java_path not found in paths.properties"
    exit 1
fi

if [[ -z "$GRADLE_JAR" ]]; then
    echo "ERROR: gradle_launcher not found in paths.properties"
    exit 1
fi

# Append executable/file names
JAVA_BIN="$JAVA_BIN/java"

# Run Gradle with all passed arguments
"$JAVA_BIN" -jar "$GRADLE_JAR" "$@"
