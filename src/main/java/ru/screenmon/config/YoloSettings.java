package ru.screenmon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки YOLO (ONNX).
 *
 * <p>Все свойства задаются в {@code application.yml} с префиксом {@code app.yolo.*}.</p>
 *
 * <p><b>Основные параметры:</b></p>
 * <ul>
 *   <li><b>enabled</b> — включить/выключить детекцию по нейросети.</li>
 *   <li><b>model-path</b> — абсолютный путь к файлу {@code best.onnx}.</li>
 *   <li><b>conf-threshold</b> — общий порог уверенности (0..1); используется, если для класса не задан свой.</li>
 *   <li><b>imgsz</b> — размер стороны изображения при подаче в модель (например 512 или 640).</li>
 *   <li><b>*-class-id</b> — номера классов в обученной модели: {@code glitches-class-id}, {@code screen-class-id}, {@code dead-pixels-block-class-id}. Должны совпадать с порядком классов в датасете (Roboflow / data.yaml). Значение -1 отключает детекцию этого класса.</li>
 *   <li><b>*-conf-threshold</b> — порог уверенности по классу (0..1): {@code glitches-conf-threshold}, {@code screen-conf-threshold}, {@code dead-pixels-block-conf-threshold}. Если 0 — берётся общий {@code conf-threshold}.</li>
 *   <li><b>glitches-frames-persist</b> — сколько кадров подряд должна быть детекция глитчей, чтобы выставить флаг (1 = без проверки подряд, 3 = только если глитчи на текущем и двух предыдущих кадрах).</li>
 *   <li><b>dead-pixels-block-frames-persist</b> — то же для битых блоков.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.yolo")
public class YoloSettings {

    /** Включена ли детекция по YOLO. */
    private boolean enabled = false;

    /** Путь к файлу модели ONNX (best.onnx). */
    private String modelPath = "";

    /** Общий порог уверенности (0..1); используется, если для класса не задан свой. */
    private double confThreshold = 0.5;

    /** Размер входа модели (сторона квадрата, например 512). */
    private int imgsz = 640;

    /** Номер класса «глитчи» в модели. Должен совпадать с датасетом. */
    private int glitchesClassId = 0;

    /** Номер класса «экран». -1 — не детектируем. */
    private int screenClassId = -1;

    /** Номер класса «битый блок» (чёрные/белые прямоугольники). -1 — не детектируем. */
    private int deadPixelsBlockClassId = -1;

    /** Порог уверенности для класса глитчи (0..1). 0 — использовать общий conf-threshold. */
    private double glitchesConfThreshold = 0.0;

    private double screenConfThreshold = 0.0;

    private double deadPixelsBlockConfThreshold = 0.0;

    /** Сколько кадров подряд с детекцией глитчей нужно для флага (1 = без устойчивости). */
    private int glitchesFramesPersist = 1;

    /** Сколько кадров подряд с детекцией битых блоков нужно для флага (1 = без устойчивости). */
    private int deadPixelsBlockFramesPersist = 1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }
    public double getConfThreshold() { return confThreshold; }
    public void setConfThreshold(double confThreshold) { this.confThreshold = confThreshold; }
    public int getImgsz() { return imgsz; }
    public void setImgsz(int imgsz) { this.imgsz = imgsz; }
    public int getGlitchesClassId() { return glitchesClassId; }
    public void setGlitchesClassId(int glitchesClassId) { this.glitchesClassId = glitchesClassId; }

    public int getScreenClassId() { return screenClassId; }
    public void setScreenClassId(int screenClassId) { this.screenClassId = screenClassId; }

    public int getDeadPixelsBlockClassId() { return deadPixelsBlockClassId; }
    public void setDeadPixelsBlockClassId(int deadPixelsBlockClassId) { this.deadPixelsBlockClassId = deadPixelsBlockClassId; }

    public double getGlitchesConfThreshold() { return glitchesConfThreshold; }
    public void setGlitchesConfThreshold(double glitchesConfThreshold) { this.glitchesConfThreshold = glitchesConfThreshold; }

    public double getScreenConfThreshold() { return screenConfThreshold; }
    public void setScreenConfThreshold(double screenConfThreshold) { this.screenConfThreshold = screenConfThreshold; }

    public double getDeadPixelsBlockConfThreshold() { return deadPixelsBlockConfThreshold; }
    public void setDeadPixelsBlockConfThreshold(double deadPixelsBlockConfThreshold) { this.deadPixelsBlockConfThreshold = deadPixelsBlockConfThreshold; }

    public int getGlitchesFramesPersist() { return glitchesFramesPersist; }
    public void setGlitchesFramesPersist(int glitchesFramesPersist) { this.glitchesFramesPersist = glitchesFramesPersist; }

    public int getDeadPixelsBlockFramesPersist() { return deadPixelsBlockFramesPersist; }
    public void setDeadPixelsBlockFramesPersist(int deadPixelsBlockFramesPersist) { this.deadPixelsBlockFramesPersist = deadPixelsBlockFramesPersist; }
}

