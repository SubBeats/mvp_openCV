package ru.screenmon.web.diagnostics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.screenmon.config.CvSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Диагностические эндпоинты для проверки OpenCV и просмотра текущих порогов CV.
 *
 * <p><b>Эндпоинты (префикс /api/demo):</b></p>
 * <ul>
 *   <li>GET /cv-settings — параметры app.cv.* (чёрный экран, фриз; битые блоки только в YOLO).</li>
 *   <li>GET /opencv — статус загрузки OpenCV.</li>
 *   <li>GET /analyze-sample — разбор тестового чёрного кадра.</li>
 *   <li>POST /analyze — разбор загруженного файла (file); в ответе meanY, var, isBlack.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/demo")
public class OpenCvDiagnosticsController {

    private final OpenCvDiagnosticsService diagnostics;
    private final CvSettings cvSettings;

    public OpenCvDiagnosticsController(OpenCvDiagnosticsService diagnostics, CvSettings cvSettings) {
        this.diagnostics = diagnostics;
        this.cvSettings = cvSettings;
    }

    @GetMapping("/cv-settings")
    public Map<String, Object> cvSettings() {
        Map<String, Object> out = new HashMap<>();
        Map<String, Object> black = new HashMap<>();
        black.put("meanThreshold", cvSettings.getBlackMeanThreshold());
        black.put("varThreshold", cvSettings.getBlackVarThreshold());
        black.put("framesInRow", cvSettings.getBlackFramesInRow());
        out.put("black", black);
        Map<String, Object> dim = new HashMap<>();
        dim.put("brightnessThreshold", cvSettings.getDimBrightnessThreshold());
        out.put("dim", dim);
        Map<String, Object> freeze = new HashMap<>();
        freeze.put("phashThreshold", cvSettings.getFreezePhashThreshold());
        freeze.put("framesMatch", cvSettings.getFreezeFramesMatch());
        freeze.put("framesWindow", cvSettings.getFreezeFramesWindow());
        out.put("freeze", freeze);
        Map<String, Object> bands = new HashMap<>();
        bands.put("deviationThreshold", cvSettings.getBandsDeviationThreshold());
        out.put("bands", bands);
        Map<String, Object> color = new HashMap<>();
        color.put("deltaEThreshold", cvSettings.getColorDeltaEThreshold());
        out.put("color", color);
        return out;
    }

    @GetMapping("/opencv")
    public Map<String, Object> opencvStatus() {
        Map<String, Object> out = new HashMap<>();
        out.put("openCvLoaded", true);
        out.put("message", "OpenCV ready. GET /api/demo/analyze-sample or POST /api/demo/analyze with file.");
        return out;
    }

    @GetMapping("/analyze-sample")
    public Map<String, Object> analyzeSample() {
        return diagnostics.analyzeSampleFrame();
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "File is required");
            return ResponseEntity.badRequest().body(err);
        }
        try {
            Path temp = Files.createTempFile("opencv_analyze_", ".jpg");
            try {
                file.transferTo(temp.toFile());
                return ResponseEntity.ok(diagnostics.analyzeImage(temp));
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}

