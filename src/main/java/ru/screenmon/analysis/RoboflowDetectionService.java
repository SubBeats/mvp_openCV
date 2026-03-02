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
 *
 * <p>Классы детекции (screen / glitches / dead-pixels-block). Нейросеть отдаёт только имя «glitches».
 * Связанные места: {@link ru.screenmon.upload.UploadController#hasClassInBoxes}, конфиг {@link ru.screenmon.config.RoboflowSettings}.</p>
 */
@Service
public class RoboflowDetectionService {

    private static final Logger LOG = Logger.getLogger(RoboflowDetectionService.class.getName());

    private static final String CLASS_SCREEN = "screen";
    private static final String CLASS_GLITCHES = "glitches";
    private static final String CLASS_DEAD_PIXELS_BLOCK = "dead-pixels-block";

    /** Ключи в JSON ответе Roboflow. */
    private static final String JSON_PREDICTIONS = "predictions";
    private static final String JSON_CONFIDENCE = "confidence";
    private static final String JSON_CLASS = "class";
    private static final String JSON_CLASS_NAME = "class_name";
    private static final String JSON_X = "x";
    private static final String JSON_Y = "y";
    private static final String JSON_WIDTH = "width";
    private static final String JSON_HEIGHT = "height";
    private static final String JSON_X_MIN = "x_min";
    private static final String JSON_Y_MIN = "y_min";
    private static final String JSON_X_MAX = "x_max";
    private static final String JSON_Y_MAX = "y_max";

    private static final double THRESHOLD_MIN = 0.0;
    private static final double THRESHOLD_MAX = 1.0;
    private static final double DEFAULT_CONFIDENCE = 0.0;
    private static final int BBOX_CENTER_DIVISOR = 2;

    private static final String IMAGE_FORMAT_JPEG = ".jpg";
    private static final String API_KEY_PARAM = "api_key";

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
        if (!isConfigurationValid()) {
            available = false;
            return;
        }
        fullInferenceUrl = buildInferenceUrlWithApiKey();
        available = true;
        LOG.info("Roboflow API enabled: " + settings.getInferenceUrl().trim());
    }

    private boolean isConfigurationValid() {
        return settings.isEnabled()
                && isNonBlank(settings.getInferenceUrl())
                && isNonBlank(settings.getApiKey());
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildInferenceUrlWithApiKey() {
        String baseUrl = settings.getInferenceUrl().trim();
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + API_KEY_PARAM + "=" + settings.getApiKey().trim();
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Отправляет кадр в Roboflow, парсит JSON и возвращает флаги по классам (как у локального YOLO).
     */
    public YoloDetectionService.Detections detect(Mat bgr) {
        if (!available || bgr == null || bgr.empty()) {
            return emptyDetections();
        }
        String base64 = encodeMatToJpegBase64(bgr);
        if (base64 == null) {
            return emptyDetections();
        }
        try {
            String responseBody = callInferenceApi(base64);
            if (responseBody == null) {
                return emptyDetectionsWithBoxes();
            }
            return buildDetectionsFromResponseJson(responseBody);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Roboflow API request failed", e);
            return emptyDetections();
        }
    }

    private static YoloDetectionService.Detections emptyDetections() {
        return new YoloDetectionService.Detections(false, false, false);
    }

    private static YoloDetectionService.Detections emptyDetectionsWithBoxes() {
        return new YoloDetectionService.Detections(false, false, false, java.util.Collections.emptyList());
    }

    private String encodeMatToJpegBase64(Mat bgr) {
        MatOfByte buffer = new MatOfByte();
        if (!Imgcodecs.imencode(IMAGE_FORMAT_JPEG, bgr, buffer)) {
            return null;
        }
        return Base64.getEncoder().encodeToString(buffer.toArray());
    }

    private String callInferenceApi(String base64Image) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>(base64Image, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(fullInferenceUrl, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }
        return response.getBody();
    }

    private YoloDetectionService.Detections buildDetectionsFromResponseJson(String json) {
        java.util.List<DetectionBox> boxes = new java.util.ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode predictionsArray = root.path(JSON_PREDICTIONS);
            if (!predictionsArray.isArray()) {
                return emptyDetectionResult(boxes);
            }

            // Флаги наличия объектов каждого класса
            DetectionFlags flags = processPredictions(predictionsArray, boxes);

            return new YoloDetectionService.Detections(flags.hasScreen, flags.hasGlitches, flags.hasDeadPixelsBlock, boxes);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Roboflow JSON parse failed", e);
            return emptyDetectionResult(boxes);
        }
    }

    private YoloDetectionService.Detections emptyDetectionResult(java.util.List<DetectionBox> boxes) {
        return new YoloDetectionService.Detections(false, false, false, boxes);
    }

    /**
     * Контейнер для флагов результата.
     */
    private static class DetectionFlags {
        boolean hasScreen = false;
        boolean hasGlitches = false;
        boolean hasDeadPixelsBlock = false;
    }

    /**
     * Обрабатывает массив предсказаний, собирает боксы и выставляет флаги.
     */
    private DetectionFlags processPredictions(JsonNode predictionsArray, java.util.List<DetectionBox> boxes) {
        DetectionFlags flags = new DetectionFlags();

        for (JsonNode predictionNode : predictionsArray) {
            String predictedClassName = readClassNameFromPrediction(predictionNode);
            double confidence = readConfidenceFromPrediction(predictionNode);
            double minConfidence = getMinConfidenceForClass(
                    predictedClassName, settings.getScreenClassName(), 
                    settings.getGlitchesClassName(), settings.getDeadPixelsBlockClassName(), 
                    settings.getConfThreshold()
            );

            if (confidence < minConfidence) {
                continue;
            }

            double[] boundingBox = readBoundingBoxFromPrediction(predictionNode);
            if (boundingBox == null) {
                continue;
            }

            boxes.add(new DetectionBox(predictedClassName, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], confidence));

            if (matchesScreenClass(predictedClassName, settings.getScreenClassName())) flags.hasScreen = true;
            if (matchesGlitchesClass(predictedClassName, settings.getGlitchesClassName())) flags.hasGlitches = true;
            if (matchesDeadPixelsBlockClass(predictedClassName, settings.getDeadPixelsBlockClassName())) flags.hasDeadPixelsBlock = true;
        }
        return flags;
    }

    private static String readClassNameFromPrediction(JsonNode predictionNode) {
        if (predictionNode.has(JSON_CLASS)) {
            return predictionNode.get(JSON_CLASS).asText("");
        }
        return predictionNode.path(JSON_CLASS_NAME).asText("");
    }

    private static double readConfidenceFromPrediction(JsonNode predictionNode) {
        return predictionNode.path(JSON_CONFIDENCE).asDouble(DEFAULT_CONFIDENCE);
    }

    /** Возвращает [x, y, width, height] или null, если формат bbox неизвестен. */
    private static double[] readBoundingBoxFromPrediction(JsonNode predictionNode) {
        if (predictionNode.has(JSON_X) && predictionNode.has(JSON_Y) && predictionNode.has(JSON_WIDTH) && predictionNode.has(JSON_HEIGHT)) {
            return parseBoundingBoxFromCenterFormat(predictionNode);
        }
        if (predictionNode.has(JSON_X_MIN) && predictionNode.has(JSON_Y_MIN) && predictionNode.has(JSON_X_MAX) && predictionNode.has(JSON_Y_MAX)) {
            return parseBoundingBoxFromMinMaxFormat(predictionNode);
        }
        return null;
    }

    private static double[] parseBoundingBoxFromCenterFormat(JsonNode node) {
        double centerX = node.get(JSON_X).asDouble();
        double centerY = node.get(JSON_Y).asDouble();
        double width = node.get(JSON_WIDTH).asDouble();
        double height = node.get(JSON_HEIGHT).asDouble();
        double left = centerX - width / BBOX_CENTER_DIVISOR;
        double top = centerY - height / BBOX_CENTER_DIVISOR;
        return new double[]{left, top, width, height};
    }

    private static double[] parseBoundingBoxFromMinMaxFormat(JsonNode node) {
        double left = node.get(JSON_X_MIN).asDouble();
        double top = node.get(JSON_Y_MIN).asDouble();
        double width = node.get(JSON_X_MAX).asDouble() - left;
        double height = node.get(JSON_Y_MAX).asDouble() - top;
        return new double[]{left, top, width, height};
    }

    private double getMinConfidenceForClass(String predictedClassName, String configuredScreen, String configuredGlitches, String configuredDeadBlock, double defaultThreshold) {
        if (predictedClassName == null) {
            return defaultThreshold;
        }
        Double threshold = null;
        if (matchesScreenClass(predictedClassName, configuredScreen)) {
            threshold = settings.getConfThresholdScreen();
        } else if (matchesGlitchesClass(predictedClassName, configuredGlitches)) {
            threshold = settings.getConfThresholdGlitches();
        } else if (matchesDeadPixelsBlockClass(predictedClassName, configuredDeadBlock)) {
            threshold = settings.getConfThresholdDeadPixelsBlock();
        }
        if (threshold == null) {
            return defaultThreshold;
        }
        return Math.max(THRESHOLD_MIN, Math.min(THRESHOLD_MAX, threshold));
    }

    private static boolean matchesClass(String predictedClass, String canonicalClassName, String configuredClassName) {
        if (predictedClass == null) return false;
        String normalized = predictedClass.trim().toLowerCase();
        if (canonicalClassName.equals(normalized)) return true;
        return configuredClassName != null && configuredClassName.equalsIgnoreCase(predictedClass.trim());
    }

    private static boolean matchesGlitchesClass(String predictedClass, String configuredGlitchesClassName) {
        return matchesClass(predictedClass, CLASS_GLITCHES, configuredGlitchesClassName);
    }

    private static boolean matchesScreenClass(String predictedClass, String configuredScreenClassName) {
        return matchesClass(predictedClass, CLASS_SCREEN, configuredScreenClassName);
    }

    private static boolean matchesDeadPixelsBlockClass(String predictedClass, String configuredDeadBlockClassName) {
        return matchesClass(predictedClass, CLASS_DEAD_PIXELS_BLOCK, configuredDeadBlockClassName);
    }
}
