package ru.screenmon.analysis;

import java.util.Objects;

/**
 * Один обнаруженный объект нейросетью (YOLO/Roboflow): класс и прямоугольник в координатах изображения.
 * <p>
 * Координаты в пикселях исходного кадра: (x, y) — левый верхний угол, width и height — размер бокса.
 * Используется для отрисовки рамок на фото во фронте и в ответе API /upload.
 * </p>
 */
public class DetectionBox {

    /** Имя класса из модели: screen, glitches, dead-pixels-block (или glitch у Roboflow). */
    private final String classLabel;
    /** Левая граница бокса (пиксели). */
    private final double x;
    /** Верхняя граница бокса (пиксели). */
    private final double y;
    private final double width;
    private final double height;
    /** Уверенность модели 0..1. */
    private final double confidence;

    public DetectionBox(String classLabel, double x, double y, double width, double height, double confidence) {
        this.classLabel = classLabel != null ? classLabel : "";
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
    }

    public String getClassLabel() { return classLabel; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getConfidence() { return confidence; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectionBox that = (DetectionBox) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0
                && Double.compare(that.width, width) == 0 && Double.compare(that.height, height) == 0
                && Double.compare(that.confidence, confidence) == 0 && Objects.equals(classLabel, that.classLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classLabel, x, y, width, height, confidence);
    }
}
