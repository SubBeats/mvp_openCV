# API

Base: `http://localhost:8080` (порт в `application.yml`).

**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Все ответы — JSON. Ошибки: `{ "error": "текст" }`.

---

## Экраны

| Method | Path | Описание |
|--------|------|----------|
| GET | `/api/screens` | Список экранов. Ответ: массив `{ id, name, createdAt }`. id — число (PK). |
| POST | `/api/screens` | Регистрация экрана. Body: `{ "name": "..." }`. Ответ: `{ id, name, message }`. id генерируется. |

---

## Загрузка кадра

| Method | Path | Описание |
|--------|------|----------|
| POST | `/upload` | Загрузка кадра. **Form-data:** `file` (обязательно), `screen_id` (обязательно, число — id экрана), `timestamp` (опционально, ISO-8601). Ответ: `{ id, timestamp, savedPath, analysis: { black, freeze, deadPixels } }`. 400 — экран не найден. |

Файлы: `storage/<id>/<YYYY-MM-DD>/<filename>.jpg`.

---

## Демо / анализ (без сохранения в H2)

| Method | Path | Описание |
|--------|------|----------|
| GET | `/api/demo/opencv` | Проверка загрузки OpenCV. `{ openCvLoaded, message }`. |
| GET | `/api/demo/analyze-sample` | Анализ сгенерированного чёрного кадра. `{ ok, width, height, meanY, std, var, isBlack, openCvUsed }`. |
| POST | `/api/demo/analyze` | Анализ переданного изображения. **Form-data:** `file`. Тот же формат ответа, что у analyze-sample. |
| GET | `/api/demo/cv-settings` | Текущие пороги CV. Вложенный JSON: `black`, `freeze`, `bands`, `deadPixels`, `color`. |

---

## Коды

- 200 — OK  
- 400 — неверный запрос (screen_id, валидация)  
- 409 — конфликт (экран уже зарегистрирован)  
- 500 — ошибка сервера  
