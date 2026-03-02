#!/usr/bin/env bash
# Запуск приложения из собранного JAR
cd "$(dirname "$0")/.."
JAR=$(ls target/mvp-opencv-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "JAR не найден. Сначала выполните: ./scripts/build.sh"
  exit 1
fi
java -jar "$JAR"
