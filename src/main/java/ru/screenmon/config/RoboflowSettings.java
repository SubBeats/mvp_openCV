package ru.screenmon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки детекции через Roboflow Inference API (по URL, без скачивания весов).
 *
 * <p>Используется, когда веса экспортировать нельзя: приложение шлёт кадр в Roboflow по HTTP и получает JSON с предсказаниями.</p>
 *
 * <p>Параметры в {@code application.yml} с префиксом {@code app.roboflow.*}:</p>
 * <ul>
 *   <li><b>enabled</b> — включить детекцию через API (если true и url задан — используется вместо локального YOLO при приоритете).</li>
 *   <li><b>inference-url</b> — URL инференса без api_key (например {@code https://detect.roboflow.com/find-screen-glitches/9}).</li>
 *   <li><b>api-key</b> — API-ключ Roboflow (добавляется к запросу как {@code api_key=...}).</li>
 *   <li><b>conf-threshold</b> — минимальная уверенность (0..1) по умолчанию для всех классов.</li>
 *   <li><b>conf-threshold-screen</b>, <b>conf-threshold-glitches</b>, <b>conf-threshold-dead-pixels-block</b> — пороги по классам (0..1); если не заданы — используется conf-threshold.</li>
 *   <li><b>*-class-name</b> — имена классов в ответе API для маппинга на экран/глитчи/битые блоки (например {@code glitch}, {@code screen}).</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.roboflow")
public class RoboflowSettings {

    private boolean enabled = false;

    /** URL инференса без api_key, например https://detect.roboflow.com/find-screen-glitches/9 */
    private String inferenceUrl = "https://serverless.roboflow.com/find-screen-glitches/9";

    /** API-ключ Roboflow. Задаётся через app.roboflow.api-key в yml или переменную окружения APP_ROBOFLOW_API_KEY. В репозитории не хранить. */
    private String apiKey = "";

    private double confThreshold = 0.58;

    /** Порог уверенности для класса «экран» (0..1). Если не задан — используется confThreshold. */
    private Double confThresholdScreen = 0.8;
    /** Порог уверенности для класса «глитчи» (0..1). Если не задан — используется confThreshold. */
    private Double confThresholdGlitches = 0.58;
    /** Порог уверенности для класса «битый блок» (0..1). Если не задан — используется confThreshold. */
    private Double confThresholdDeadPixelsBlock = 0.53;

    /** Имя класса «глитчи» в ответе API (например glitch). */
    private String glitchesClassName = "glitch";

    /** Имя класса «экран». Пусто — не детектируем. */
    private String screenClassName = "screen";

    /** Имя класса «битый блок». Пусто — не детектируем. */
    private String deadPixelsBlockClassName = "dead-pixels-block";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getInferenceUrl() { return inferenceUrl; }
    public void setInferenceUrl(String inferenceUrl) { this.inferenceUrl = inferenceUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public double getConfThreshold() { return confThreshold; }
    public void setConfThreshold(double confThreshold) { this.confThreshold = confThreshold; }
    public Double getConfThresholdScreen() { return confThresholdScreen; }
    public void setConfThresholdScreen(Double confThresholdScreen) { this.confThresholdScreen = confThresholdScreen; }
    public Double getConfThresholdGlitches() { return confThresholdGlitches; }
    public void setConfThresholdGlitches(Double confThresholdGlitches) { this.confThresholdGlitches = confThresholdGlitches; }
    public Double getConfThresholdDeadPixelsBlock() { return confThresholdDeadPixelsBlock; }
    public void setConfThresholdDeadPixelsBlock(Double confThresholdDeadPixelsBlock) { this.confThresholdDeadPixelsBlock = confThresholdDeadPixelsBlock; }
    public String getGlitchesClassName() { return glitchesClassName; }
    public void setGlitchesClassName(String glitchesClassName) { this.glitchesClassName = glitchesClassName; }
    public String getScreenClassName() { return screenClassName; }
    public void setScreenClassName(String screenClassName) { this.screenClassName = screenClassName; }
    public String getDeadPixelsBlockClassName() { return deadPixelsBlockClassName; }
    public void setDeadPixelsBlockClassName(String deadPixelsBlockClassName) { this.deadPixelsBlockClassName = deadPixelsBlockClassName; }
}
