#!/usr/bin/env sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
MAVEN_VERSION="3.9.9"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_ZIP="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if ! command -v java >/dev/null 2>&1; then
  echo "Java was not found on PATH. Install JDK 17 or set JAVA_HOME, then run this command again." >&2
  exit 1
fi

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$WRAPPER_DIR"
  echo "Downloading Apache Maven $MAVEN_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$MAVEN_URL" -o "$MAVEN_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$MAVEN_URL" -O "$MAVEN_ZIP"
  else
    echo "Neither curl nor wget is available to download Maven." >&2
    exit 1
  fi
  rm -rf "$MAVEN_HOME"
  unzip -q "$MAVEN_ZIP" -d "$WRAPPER_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
