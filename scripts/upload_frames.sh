#!/usr/bin/env bash
set -euo pipefail

# Базовый скрипт загрузки изображений для анализа.
# Настраиваешь URL сервиса и список картинок — запускаешь, ответы приходят в терминал.
#
# Требования: bash, curl.

BASE_URL="http://localhost:8090"
SCREEN_ID="1"

# Абсолютные пути к картинкам (можно сколько угодно).
IMAGES=(
  "/Users/bulat/Downloads/Find_screen_glitches.v1i.yolov11/train/images/-3544_8754_876aa822_e262_3323_bb05_248e21e52f35_1400x720_fish-1-_jpg.rf.71d44244984295a31396da050cc688f2.jpg"
  "/Users/bulat/Downloads/Find_screen_glitches.v1i.yolov11/train/images/-3414_8527_1a1295d6_84f1_3037_b36e_c52a22692fd0_1140kh720_3_16_26_jpg.rf.80e56f80e7c48ee5f5ea39004a3d2153.jpg"
)

if [[ ${#IMAGES[@]} -lt 1 ]]; then
  echo "No images configured. Edit IMAGES=(...) in this script." >&2
  exit 1
fi

for file_path in "${IMAGES[@]}"; do
  if [[ ! -f "$file_path" ]]; then
    echo "File not found: $file_path" >&2
    exit 4
  fi

  echo
  echo "Uploading: $file_path"
  curl -sS -X POST "${BASE_URL}/upload" \
    -F "screen_id=${SCREEN_ID}" \
    -F "file=@${file_path}"
  echo
done

