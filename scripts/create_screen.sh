#!/usr/bin/env bash
set -euo pipefail

# Базовый скрипт загрузки изображений для анализа.
# Настраиваешь URL сервиса и список картинок — запускаешь, ответы приходят в терминал.
#
# Требования: bash, curl.

BASE_URL="http://localhost:8090"
SCREEN_NAME="test"

echo "Creating screen: $SCREEN_NAME"
curl -sS -X POST "${BASE_URL}/api/screens" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"${SCREEN_NAME}\"}"
echo

