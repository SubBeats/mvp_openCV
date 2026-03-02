package ru.screenmon.analysis;

import ru.screenmon.storage.FrameAnalysis;

import java.util.Collections;
import java.util.List;

/**
 * Результат анализа одного кадра: что записали в БД и боксы для отрисовки на фото.
 * <p>
 * Возвращается из {@link FrameAnalysisService#analyzeAndSave}; боксы передаются в ответ POST /upload,
 * чтобы фронт мог нарисовать рамки поверх загруженного изображения.
 * </p>
 */
public class AnalysisResult {

    /** Сохранённая запись в frame_analysis (флаги, метрики). Может быть null при ошибке загрузки кадра. */
    private final FrameAnalysis analysis;
    /** Список детекций с координатами (экран, глитчи, битые блоки). Пустой список, если нейросеть не вызывалась или ничего не нашла. */
    private final List<DetectionBox> boxes;

    public AnalysisResult(FrameAnalysis analysis, List<DetectionBox> boxes) {
        this.analysis = analysis;
        this.boxes = boxes != null ? boxes : Collections.emptyList();
    }

    public FrameAnalysis getAnalysis() { return analysis; }
    public List<DetectionBox> getBoxes() { return boxes; }
}
