/**
 * Конфигурация приложения.
 *
 * <p><b>Структура:</b> классы настроек, значения которых берутся из {@code application.yml}
 * (префиксы {@code app.cv.*} и {@code app.yolo.*}).</p>
 *
 * <p><b>Основные классы и параметры:</b></p>
 * <ul>
 *   <li>{@link ru.screenmon.config.CvSettings} — пороги для OpenCV-эвристик (чёрный экран, фриз). Битые блоки — только YOLO.</li>
 *   <li>{@link ru.screenmon.config.YoloSettings} — путь к ONNX-модели, пороги уверенности по классам, номера классов (screen, glitches, dead-pixels-block).</li>
 * </ul>
 */
package ru.screenmon.config;
