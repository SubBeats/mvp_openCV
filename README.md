# Screen Monitor MVP

Минимальный MVP мониторинга уличных видеоэкранов: загрузка кадров с камер и анализ через OpenCV (яркость, потухший экран, freeze, битые пиксели). H2 для хранения экранов и анализа.

## Требования

- JDK 8+, Maven 3.8+

## Запуск

```bash
mvn spring-boot:run
```

Приложение: **http://localhost:8090**  
**Swagger UI:** http://localhost:8090/swagger-ui.html

## API-ключ Roboflow (переменная окружения)

Ключ **не хранится в репозитории**. Задаётся при запуске:

- **Локально:** `export APP_ROBOFLOW_API_KEY=твой_ключ` или файл `application-local.yml` (в .gitignore) с `app.roboflow.api-key: твой_ключ`.
- **Render:** в панели сервиса → **Environment** → добавить переменную **APP_ROBOFLOW_API_KEY**, значение — ключ из [Roboflow](https://app.roboflow.com) (Account → API keys).

Локально: `export APP_ROBOFLOW_API_KEY=ключ` перед `mvn spring-boot:run` или `application-local.yml` (в .gitignore).

## Как пользоваться

1. **Регистрация экрана** (один раз):  
   `curl -X POST http://localhost:8090/api/screens -H "Content-Type: application/json" -d '{"name":"Угол улицы"}'`  
   В ответе будет `id` (число).

2. **Загрузка кадра**:  
   `curl -X POST http://localhost:8090/upload -F "file=@image.jpg" -F "screen_id=1"`  
   `screen_id` — число (id экрана из GET /api/screens или ответа регистрации). В ответе: путь и анализ (black, freeze, deadPixels).

3. **Веб-форма**: открой **http://localhost:8090/** — введи id экрана (число), выбери файл, нажми «Загрузить и проанализировать».

## API

- **GET /api/screens** — список экранов
- **POST /api/screens** — регистрация экрана (JSON: name), в ответе id
- **POST /upload** — загрузка кадра (form-data: file, screen_id — число)
- **GET /api/demo/opencv** — проверка OpenCV
- **GET /api/demo/analyze-sample** — тестовый анализ
- **POST /api/demo/analyze** — анализ файла (form-data: file)
- **GET /api/demo/cv-settings** — настройки CV

Кадры: **storage/<id>/<YYYY-MM-DD>/<filename>.jpg**. H2: **./data/screenmon.mv.db**.

## H2: просмотр и правка БД

1. Запусти приложение (`mvn spring-boot:run`).
2. Открой в браузере: **http://localhost:8090/h2-console**
3. На странице входа:
   - **JDBC URL:** `jdbc:h2:file:./data/screenmon` (если запускаешь из корня проекта). Или полный путь, например: `jdbc:h2:file:/Users/.../mvp_openCV/data/screenmon`
   - **User Name:** `sa`
   - **Password:** пусто
4. Нажми **Connect** — откроется веб-интерфейс H2 (SQL, таблицы, выполнение запросов).
5. Таблицы: **SCREEN** (id, name, created_at), **FRAME_ANALYSIS** (id, screen_id, ts, frame_path, mean_y, var_y, phash, is_black, is_freeze, dead_pixels_detected, created_at). Можно выполнять `SELECT * FROM SCREEN;`, `UPDATE`, `DELETE` и т.д.
6. Схема пересоздаётся при каждом старте: `spring.jpa.hibernate.ddl-auto: create-drop`. Данные H2 не сохраняются между перезапусками.

## Настройка детекции (OpenCV)

Подробное описание пайплайна, формул и порогов — в **[OPENCV_ANALYSIS.md](OPENCV_ANALYSIS.md)**. Там же чеклист для проверки яркости, фриза и битых пикселей на тестовых данных. Настройки в `application.yml` (секция `app.cv`) и в ответе **GET /api/demo/cv-settings**.
