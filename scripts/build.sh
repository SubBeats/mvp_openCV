#!/usr/bin/env bash
# Сборка JAR для запуска приложения
cd "$(dirname "$0")/.."
mvn clean package -DskipTests
echo "JAR: target/mvp-opencv-0.1.0-SNAPSHOT.jar"
