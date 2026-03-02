package ru.screenmon.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.screenmon.analysis.DetectionBox;
import ru.screenmon.storage.FrameAnalysis;
import ru.screenmon.storage.FrameAnalysisRepository;
import ru.screenmon.storage.Screen;
import ru.screenmon.storage.ScreenRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API регистрации экранов и загрузки кадров.
 *
 * <p><b>Эндпоинты:</b></p>
 * <ul>
 *   <li>{@code GET /api/screens} — список зарегистрированных экранов (id, name, createdAt).</li>
 *   <li>{@code POST /api/screens} — регистрация экрана; тело {@code {"name": "Название"}}; в ответе — id.</li>
 *   <li>{@code POST /upload} — загрузка кадра; параметры: {@code file} (обязательно), {@code screen_id} (обязательно), {@code timestamp} (опционально). В ответе — savedPath, analysis (black, dim, freeze — OpenCV; yoloGlitches, yoloScreen, yoloDeadPixelsBlock — нейросеть).</li>
 * </ul>
 */
@RestController
@RequestMapping
public class UploadController {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());

    /** Ключи тела запроса (POST /api/screens). */
    private static final String BODY_NAME = "name";

    /** Параметры запроса (POST /upload). */
    private static final String PARAM_FILE = "file";
    private static final String PARAM_SCREEN_ID = "screen_id";
    private static final String PARAM_TIMESTAMP = "timestamp";

    /** Ключи ответа API (status, screens, upload). */
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_LAST_TS = "lastTs";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ANALYSIS = "analysis";
    private static final String KEY_MEAN_Y = "meanY";
    private static final String KEY_VAR_Y = "varY";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SAVED_PATH = "savedPath";
    private static final String KEY_BOXES = "boxes";
    private static final String KEY_ERROR = "error";

    /** Ключи внутри analysis. */
    private static final String KEY_BLACK = "black";
    private static final String KEY_DIM = "dim";
    private static final String KEY_FREEZE = "freeze";
    private static final String KEY_YOLO_GLITCHES = "yoloGlitches";
    private static final String KEY_YOLO_SCREEN = "yoloScreen";
    private static final String KEY_YOLO_DEAD_PIXELS_BLOCK = "yoloDeadPixelsBlock";

    /** Ключи внутри элемента boxes[]. */
    private static final String KEY_CLASS = "class";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_CONFIDENCE = "confidence";

    /** Имена классов детекции (нейросеть отдаёт только glitches). */
    private static final String CLASS_GLITCHES = "glitches";
    private static final String CLASS_SCREEN = "screen";
    private static final String CLASS_DEAD_PIXELS_BLOCK = "dead-pixels-block";

    private static final String DEFAULT_SCREEN_NAME_PREFIX = "Экран ";
    private static final String MESSAGE_SCREEN_REGISTERED = "Экран зарегистрирован";

    private final FrameUploadHandler frameUploadHandler;
    private final ScreenRepository screenRepository;
    private final FrameAnalysisRepository frameAnalysisRepository;

    public UploadController(FrameUploadHandler frameUploadHandler,
                            ScreenRepository screenRepository,
                            FrameAnalysisRepository frameAnalysisRepository) {
        this.frameUploadHandler = frameUploadHandler;
        this.screenRepository = screenRepository;
        this.frameAnalysisRepository = frameAnalysisRepository;
    }

    /**
     * Список экранов с последним результатом анализа (для страницы «Статус сети»).
     */
    @GetMapping("/api/status")
    public List<Map<String, Object>> status() {
        List<Screen> screens = screenRepository.findAllByOrderByIdAsc();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Screen s : screens) {
            Map<String, Object> m = new HashMap<>();
            m.put(KEY_ID, s.getId());
            m.put(KEY_NAME, s.getName() != null ? s.getName() : (DEFAULT_SCREEN_NAME_PREFIX + s.getId()));
            FrameAnalysis last = frameAnalysisRepository.findTop1ByScreenIdOrderByTsDesc(s.getId());
            if (last != null) {
                m.put(KEY_LAST_TS, last.getTs() != null ? ISO_LOCAL.format(last.getTs()) : null);
                Map<String, Object> a = new HashMap<>();
                a.put(KEY_BLACK, Boolean.TRUE.equals(last.getIsBlack()));
                a.put(KEY_DIM, Boolean.TRUE.equals(last.getIsDim()));
                a.put(KEY_FREEZE, Boolean.TRUE.equals(last.getIsFreeze()));
                a.put(KEY_YOLO_GLITCHES, Boolean.TRUE.equals(last.getYoloGlitchesDetected()));
                a.put(KEY_YOLO_SCREEN, Boolean.TRUE.equals(last.getYoloScreenDetected()));
                a.put(KEY_YOLO_DEAD_PIXELS_BLOCK, Boolean.TRUE.equals(last.getYoloDeadPixelsBlockDetected()));
                m.put(KEY_ANALYSIS, a);
                m.put(KEY_MEAN_Y, last.getMeanY());
                m.put(KEY_VAR_Y, last.getVarY());
            } else {
                m.put(KEY_LAST_TS, null);
                m.put(KEY_ANALYSIS, null);
            }
            out.add(m);
        }
        return out;
    }

    @GetMapping("/api/screens")
    public List<Map<String, Object>> listScreens() {
        List<Screen> screens = screenRepository.findAllByOrderByIdAsc();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Screen s : screens) {
            Map<String, Object> m = new HashMap<>();
            m.put(KEY_ID, s.getId());
            m.put(KEY_NAME, s.getName());
            m.put(KEY_CREATED_AT, s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            out.add(m);
        }
        return out;
    }

    @PostMapping("/api/screens")
    public ResponseEntity<?> registerScreen(@RequestBody Map<String, String> body) {
        String name = body != null ? body.get(BODY_NAME) : null;
        Screen screen = new Screen(name != null ? name.trim() : null);
        screenRepository.save(screen);

        Map<String, Object> ok = new HashMap<>();
        ok.put(KEY_ID, screen.getId());
        ok.put(KEY_NAME, screen.getName());
        ok.put(KEY_MESSAGE, MESSAGE_SCREEN_REGISTERED);
        return ResponseEntity.ok(ok);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFrame(
        @RequestParam(PARAM_FILE) MultipartFile file,
        @RequestParam(value = PARAM_SCREEN_ID, required = false) Long screenId,
        @RequestParam(value = PARAM_TIMESTAMP, required = false) String timestampStr
    ) {
        if (screenId == null) {
            return ResponseEntity.badRequest().body(error("screen_id обязателен (число — id экрана)."));
        }
        if (!screenRepository.existsById(screenId)) {
            return ResponseEntity.badRequest().body(error("Экран не найден. GET /api/screens — затем укажи id в screen_id."));
        }

        Instant ts = parseTimestamp(timestampStr);

        try {
            UploadResult result = frameUploadHandler.saveAndAnalyze(screenId, ts, file);
            return ResponseEntity.ok(toMap(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(e.getMessage()));
        }
    }

    private static Instant parseTimestamp(String timestampStr) {
        if (timestampStr != null && !timestampStr.trim().isEmpty()) {
            try {
                return Instant.parse(timestampStr.trim());
            } catch (Exception ignored) {
            }
        }
        return Instant.now();
    }

    private static Map<String, Object> toMap(UploadResult r) {
        Map<String, Object> m = new HashMap<>();
        m.put(KEY_ID, r.getScreenId());
        m.put(KEY_TIMESTAMP, r.getTimestamp().toString());
        m.put(KEY_SAVED_PATH, r.getSavedPath());
        if (r.getAnalysis() != null) {
            Map<String, Object> a = new HashMap<>();
            a.put(KEY_BLACK, r.getAnalysis().isBlack());
            a.put(KEY_DIM, r.getAnalysis().isDim());
            a.put(KEY_FREEZE, r.getAnalysis().isFreeze());
            boolean fromBoxesGlitches = hasClassInBoxes(r.getBoxes(), CLASS_GLITCHES);
            boolean fromBoxesScreen = hasClassInBoxes(r.getBoxes(), CLASS_SCREEN);
            boolean fromBoxesDead = hasClassInBoxes(r.getBoxes(), CLASS_DEAD_PIXELS_BLOCK);
            a.put(KEY_YOLO_GLITCHES, fromBoxesGlitches || r.getAnalysis().isYoloGlitches());
            a.put(KEY_YOLO_SCREEN, fromBoxesScreen || r.getAnalysis().isYoloScreen());
            a.put(KEY_YOLO_DEAD_PIXELS_BLOCK, fromBoxesDead || r.getAnalysis().isYoloDeadPixelsBlock());
            m.put(KEY_ANALYSIS, a);
        }
        if (r.getBoxes() != null && !r.getBoxes().isEmpty()) {
            List<Map<String, Object>> boxList = new ArrayList<>();
            for (DetectionBox b : r.getBoxes()) {
                Map<String, Object> box = new HashMap<>();
                box.put(KEY_CLASS, b.getClassLabel());
                box.put(KEY_X, b.getX());
                box.put(KEY_Y, b.getY());
                box.put(KEY_WIDTH, b.getWidth());
                box.put(KEY_HEIGHT, b.getHeight());
                box.put(KEY_CONFIDENCE, b.getConfidence());
                boxList.add(box);
            }
            m.put(KEY_BOXES, boxList);
        }
        return m;
    }

    private static boolean hasClassInBoxes(List<DetectionBox> boxes, String... classNames) {
        if (boxes == null || boxes.isEmpty()) return false;
        for (DetectionBox b : boxes) {
            String cls = b.getClassLabel();
            if (cls == null) continue;
            for (String name : classNames) {
                if (name != null && name.equalsIgnoreCase(cls)) return true;
            }
        }
        return false;
    }

    private static Map<String, String> error(String message) {
        Map<String, String> e = new HashMap<>();
        e.put(KEY_ERROR, message);
        return e;
    }
}
