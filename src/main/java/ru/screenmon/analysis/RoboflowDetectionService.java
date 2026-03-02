package ru.screenmon.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.screenmon.config.RoboflowSettings;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Детекция через Roboflow Inference API: отправка кадра по URL (base64), получение JSON с предсказаниями.
 *
 * <p>Используется как первая версия, когда веса модели скачать нельзя. Возвращает тот же тип {@link YoloDetectionService.Detections}, что и локальный YOLO.</p>
 */
@Service
public class RoboflowDetectionService {

    private static final Logger LOG = Logger.getLogger(RoboflowDetectionService.class.getName());

    private final RoboflowSettings settings;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String fullInferenceUrl;
    private boolean available;

    public RoboflowDetectionService(RoboflowSettings settings) {
        this.settings = settings;
    }

    @PostConstruct
    public void init() {
        if (!settings.isEnabled() || settings.getInferenceUrl() == null || settings.getInferenceUrl().trim().isEmpty()
                || settings.getApiKey() == null || settings.getApiKey().trim().isEmpty()) {
            available = false;
            return;
        }
        String url = settings.getInferenceUrl().trim();
        String sep = url.contains("?") ? "&" : "?";
        fullInferenceUrl = url + sep + "api_key=" + settings.getApiKey().trim();
        available = true;
        LOG.info("Roboflow API enabled: " + url);
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Отправляет кадр в Roboflow, парсит JSON и возвращает флаги по классам (как у локального YOLO).
     */
    public YoloDetectionService.Detections detect(Mat bgr) {
        if (!available || bgr == null || bgr.empty()) {
            return new YoloDetectionService.Detections(false, false, false);
        }
        String base64 = matToJpegBase64(bgr);
        if (base64 == null) return new YoloDetectionService.Detections(false, false, false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>(base64, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(fullInferenceUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new YoloDetectionService.Detections(false, false, false, java.util.Collections.emptyList());
            }
            return parsePredictions(response.getBody());
        } catch (Exception e) {
            LOG.log(Level.FINE, "Roboflow API request failed", e);
            return new YoloDetectionService.Detections(false, false, false);
        }
    }

    private String matToJpegBase64(Mat bgr) {
        MatOfByte buf = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", bgr, buf)) return null;
        return Base64.getEncoder().encodeToString(buf.toArray());
    }

    private YoloDetectionService.Detections parsePredictions(String json) {
        double defaultThreshold = Math.max(0, Math.min(1, settings.getConfThreshold()));
        boolean screen = false;
        boolean glitches = false;
        boolean deadPixelsBlock = false;
        java.util.List<DetectionBox> boxes = new java.util.ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode predictions = root.path("predictions");
            if (!predictions.isArray()) return new YoloDetectionService.Detections(false, false, false, boxes);

            String glitchName = nullIfEmpty(settings.getGlitchesClassName());
            String screenName = nullIfEmpty(settings.getScreenClassName());
            String deadName = nullIfEmpty(settings.getDeadPixelsBlockClassName());

            for (JsonNode pred : predictions) {
                double conf = pred.path("confidence").asDouble(0);
                String cls = pred.has("class") ? pred.get("class").asText("") : pred.path("class_name").asText("");
                double threshold = thresholdForClass(cls, screenName, glitchName, deadName, defaultThreshold);
                if (conf < threshold) {
                    continue;
                }

                double x, y, w, h;
                if (pred.has("x") && pred.has("y") && pred.has("width") && pred.has("height")) {
                    double cx = pred.get("x").asDouble();
                    double cy = pred.get("y").asDouble();
                    w = pred.get("width").asDouble();
                    h = pred.get("height").asDouble();
                    x = cx - w / 2;
                    y = cy - h / 2;
                } else if (pred.has("x_min") && pred.has("y_min") && pred.has("x_max") && pred.has("y_max")) {
                    x = pred.get("x_min").asDouble();
                    y = pred.get("y_min").asDouble();
                    w = pred.get("x_max").asDouble() - x;
                    h = pred.get("y_max").asDouble() - y;
                } else {
                    continue;
                }
                boxes.add(new DetectionBox(cls, x, y, w, h, conf));

                if (glitchName != null && glitchName.equalsIgnoreCase(cls)) glitches = true;
                if (screenName != null && screenName.equalsIgnoreCase(cls)) screen = true;
                if (deadName != null && deadName.equalsIgnoreCase(cls)) deadPixelsBlock = true;
            }
            return new YoloDetectionService.Detections(screen, glitches, deadPixelsBlock, boxes);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Roboflow JSON parse failed", e);
            return new YoloDetectionService.Detections(false, false, false, boxes);
        }
    }

    private double thresholdForClass(String cls, String screenName, String glitchName, String deadName, double defaultThreshold) {
        if (cls == null) return defaultThreshold;
        Double thresholdConfidence;
        if (screenName != null && screenName.equalsIgnoreCase(cls)) {
            thresholdConfidence = settings.getConfThresholdScreen();
        } else if (glitchName != null && glitchName.equalsIgnoreCase(cls)) {
            thresholdConfidence = settings.getConfThresholdGlitches();
        } else if (deadName != null && deadName.equalsIgnoreCase(cls)) {
            thresholdConfidence = settings.getConfThresholdDeadPixelsBlock();
        } else {
            return defaultThreshold;
        }
        if (thresholdConfidence == null) return defaultThreshold;
        return Math.max(0, Math.min(1, thresholdConfidence));
    }

    private static String nullIfEmpty(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
