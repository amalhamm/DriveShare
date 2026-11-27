#!/bin/sh
# Compiles and runs DriveShare (macOS / Linux). Requires a JDK 11 or newer.
cd "$(dirname "$0")"
CP="lib/sqlite-jdbc-3.46.1.3.jar"
mkdir -p out
javac -d out -cp "$CP" -sourcepath src src/driveshare/Main.java || exit 1
java -cp "out:$CP" driveshare.Main
