/**
 * Слой хранения данных.
 *
 * <p><b>Структура:</b> JPA-сущности и Spring Data репозитории для хранения истории анализа кадров и реестра экранов.</p>
 *
 * <p><b>Основные сущности:</b></p>
 * <ul>
 *   <li>Экраны ({@link ru.screenmon.storage.Screen}) — зарегистрированные мониторы/билборды.</li>
 *   <li>Результаты анализа кадров ({@link ru.screenmon.storage.FrameAnalysis}) — путь к кадру, флаги (чёрный экран, фриз, битые пиксели, глитчи по YOLO и т.д.), метрики (meanY, varY, pHash).</li>
 * </ul>
 *
 * <p>БД настраивается в {@code application.yml} (по умолчанию H2 file).</p>
 */
package ru.screenmon.storage;
