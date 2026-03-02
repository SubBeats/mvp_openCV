/**
 * HTTP API загрузки и регистрации.
 *
 * <p><b>Структура:</b> REST-эндпоинты для регистрации экранов и загрузки кадров с последующим анализом.</p>
 *
 * <p><b>Основные эндпоинты:</b></p>
 * <ul>
 *   <li>{@code GET /api/screens} — список зарегистрированных экранов.</li>
 *   <li>{@code POST /api/screens} — регистрация нового экрана (тело: {@code {"name": "..."}}).</li>
 *   <li>{@code POST /upload} — загрузка одного кадра (обязательные параметры: {@code file}, {@code screen_id}); в ответе — результат анализа (black, freeze, deadPixels, yoloGlitches и т.д.).</li>
 * </ul>
 *
 * <p><b>Параметры запроса /upload:</b> {@code screen_id} — id экрана; {@code file} — файл изображения; опционально {@code timestamp} в формате ISO-8601.</p>
 */
package ru.screenmon.upload;
