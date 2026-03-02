/**
 * Пакет анализа кадров.
 *
 * <p><b>Структура:</b> здесь находятся сервисы, которые по загруженному кадру вычисляют признаки
 * (чёрный экран, фриз, глитчи, битые блоки) и сохраняют результат в БД.</p>
 *
 * <p><b>Основные классы:</b></p>
 * <ul>
 *   <li>{@link ru.screenmon.analysis.FrameAnalysisService} — оркестрация: OpenCV-эвристики + вызов YOLO, сохранение в БД.</li>
 *   <li>{@link ru.screenmon.analysis.YoloDetectionService} — инференс модели YOLO (ONNX): детекция экрана, глитчей, битых блоков.</li>
 * </ul>
 *
 * <p><b>Параметры:</b> настройки OpenCV — {@code app.cv.*} в {@code application.yml};
 * настройки YOLO — {@code app.yolo.*} (путь к модели, пороги уверенности, номера классов).</p>
 */
package ru.screenmon.analysis;
