#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
OUT_DIR="$BUILD_DIR/classes"
JAR_DIR="$BUILD_DIR"
JAR_NAME="rps-server.jar"

rm -rf "$BUILD_DIR"
mkdir -p "$OUT_DIR"

# Compile
find "$ROOT_DIR/src" -name "*.java" > "$BUILD_DIR/sources.txt"

# Target 17 by default; allow override via JAVA_RELEASE=11 etc.
JAVA_RELEASE="${JAVA_RELEASE:-17}"

javac -encoding UTF-8 --release "$JAVA_RELEASE" -d "$OUT_DIR" @"$BUILD_DIR/sources.txt"

# Package
MAIN_CLASS="rps.Main"
jar --create --file "$JAR_DIR/$JAR_NAME" --manifest <(cat <<EOF
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS

EOF
) -C "$OUT_DIR" .

echo "Built: $JAR_DIR/$JAR_NAME"
