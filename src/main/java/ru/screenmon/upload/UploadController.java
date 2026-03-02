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
            m.put("id", s.getId());
            m.put("name", s.getName() != null ? s.getName() : ("Экран " + s.getId()));
            FrameAnalysis last = frameAnalysisRepository.findTop1ByScreenIdOrderByTsDesc(s.getId());
            if (last != null) {
                m.put("lastTs", last.getTs() != null ? ISO_LOCAL.format(last.getTs()) : null);
                Map<String, Object> a = new HashMap<>();
                a.put("black", Boolean.TRUE.equals(last.getIsBlack()));
                a.put("dim", Boolean.TRUE.equals(last.getIsDim()));
                a.put("freeze", Boolean.TRUE.equals(last.getIsFreeze()));
                a.put("yoloGlitches", Boolean.TRUE.equals(last.getYoloGlitchesDetected()));
                a.put("glitchOnLastFrame", Boolean.TRUE.equals(last.getYoloGlitchesRaw()));
                a.put("yoloScreen", Boolean.TRUE.equals(last.getYoloScreenDetected()));
                a.put("yoloDeadPixelsBlock", Boolean.TRUE.equals(last.getYoloDeadPixelsBlockDetected()));
                m.put("analysis", a);
                m.put("meanY", last.getMeanY());
                m.put("varY", last.getVarY());
            } else {
                m.put("lastTs", null);
                m.put("analysis", null);
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
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            out.add(m);
        }
        return out;
    }

    @PostMapping("/api/screens")
    public ResponseEntity<?> registerScreen(@RequestBody Map<String, String> body) {
        String name = body != null ? body.get("name") : null;
        Screen screen = new Screen(name != null ? name.trim() : null);
        screenRepository.save(screen);

        Map<String, Object> ok = new HashMap<>();
        ok.put("id", screen.getId());
        ok.put("name", screen.getName());
        ok.put("message", "Экран зарегистрирован");
        return ResponseEntity.ok(ok);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFrame(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "screen_id", required = false) Long screenId,
        @RequestParam(value = "timestamp", required = false) String timestampStr
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
        m.put("id", r.getScreenId());
        m.put("timestamp", r.getTimestamp().toString());
        m.put("savedPath", r.getSavedPath());
        if (r.getAnalysis() != null) {
            Map<String, Object> a = new HashMap<>();
            a.put("black", r.getAnalysis().isBlack());
            a.put("dim", r.getAnalysis().isDim());
            a.put("freeze", r.getAnalysis().isFreeze());
            boolean fromBoxesGlitches = hasClassInBoxes(r.getBoxes(), "glitches", "glitch");
            boolean fromBoxesScreen = hasClassInBoxes(r.getBoxes(), "screen");
            boolean fromBoxesDead = hasClassInBoxes(r.getBoxes(), "dead-pixels-block");
            a.put("yoloGlitches", fromBoxesGlitches || r.getAnalysis().isYoloGlitches());
            a.put("yoloScreen", fromBoxesScreen || r.getAnalysis().isYoloScreen());
            a.put("yoloDeadPixelsBlock", fromBoxesDead || r.getAnalysis().isYoloDeadPixelsBlock());
            m.put("analysis", a);
        }
        if (r.getBoxes() != null && !r.getBoxes().isEmpty()) {
            List<Map<String, Object>> boxList = new ArrayList<>();
            for (DetectionBox b : r.getBoxes()) {
                Map<String, Object> box = new HashMap<>();
                box.put("class", b.getClassLabel());
                box.put("x", b.getX());
                box.put("y", b.getY());
                box.put("width", b.getWidth());
                box.put("height", b.getHeight());
                box.put("confidence", b.getConfidence());
                boxList.add(box);
            }
            m.put("boxes", boxList);
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
        e.put("error", message);
        return e;
    }
}
