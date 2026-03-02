package ru.screenmon.analysis;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import ru.screenmon.config.YoloSettings;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис детекции по модели YOLO (ONNX).
 *
 * <p><b>Назначение:</b> загружает экспортированную модель (best.onnx), прогоняет кадр и возвращает флаги «есть/нет» для целевых классов (экран, глитчи, битые блоки). Координаты боксов наружу не отдаются.</p>
 *
 * <p><b>Параметры (application.yml, app.yolo.*):</b></p>
 * <ul>
 *   <li>model-path — путь к ONNX-файлу;</li>
 *   <li>imgsz — размер входа (кадр приводится к квадрату с letterbox);</li>
 *   <li>*-class-id — номера классов в модели (должны совпадать с обучением);</li>
 *   <li>*-conf-threshold — порог уверенности по классу (0..1).</li>
 * </ul>
 *
 * <p>Если модель не загружена (файл не найден или YOLO выключен), {@link #isAvailable()} возвращает false, все детекции — false.</p>
 */
@Service
public class YoloDetectionService {

    private static final Logger LOG = Logger.getLogger(YoloDetectionService.class.getName());
    private static final int CHANNELS = 3;
    private static final org.opencv.core.Scalar LETTERBOX_COLOR = new org.opencv.core.Scalar(114, 114, 114);

    private final YoloSettings settings;
    private OrtEnvironment env;
    private OrtSession session;
    private String inputName;
    private int inputHeight;
    private int inputWidth;
    private boolean available;

    public YoloDetectionService(YoloSettings settings) {
        this.settings = settings;
    }

    /**
     * Результат одного прогона модели: флаги наличия детекций и список боксов для отрисовки.
     */
    public static class Detections {
        private final boolean screen;
        private final boolean glitches;
        private final boolean deadPixelsBlock;
        private final List<DetectionBox> boxes;

        public Detections(boolean screen, boolean glitches, boolean deadPixelsBlock) {
            this(screen, glitches, deadPixelsBlock, Collections.emptyList());
        }

        public Detections(boolean screen, boolean glitches, boolean deadPixelsBlock, List<DetectionBox> boxes) {
            this.screen = screen;
            this.glitches = glitches;
            this.deadPixelsBlock = deadPixelsBlock;
            this.boxes = boxes != null ? new ArrayList<>(boxes) : new ArrayList<>();
        }

        public boolean isScreen() { return screen; }
        public boolean isGlitches() { return glitches; }
        public boolean isDeadPixelsBlock() { return deadPixelsBlock; }
        public List<DetectionBox> getBoxes() { return boxes; }
    }

    @PostConstruct
    public void init() {
        if (!settings.isEnabled() || settings.getModelPath() == null || settings.getModelPath().trim().isEmpty()) {
            available = false;
            return;
        }
        Path path = Paths.get(settings.getModelPath().trim()).toAbsolutePath();
        if (!Files.isRegularFile(path)) {
            LOG.warning("YOLO model file not found: " + path);
            available = false;
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(path.toString(), new OrtSession.SessionOptions());
            inputName = session.getInputNames().iterator().next();
            Object inputInfo = session.getInputInfo().get(inputName).getInfo();
            if (inputInfo instanceof TensorInfo) {
                long[] dims = ((TensorInfo) inputInfo).getShape();
                inputHeight = (int) dims[2];
                inputWidth = (int) dims[3];
            } else {
                inputHeight = settings.getImgsz();
                inputWidth = settings.getImgsz();
            }
            available = true;
            LOG.info("YOLO model loaded: " + path + " input " + inputWidth + "x" + inputHeight);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load YOLO model: " + path, e);
            available = false;
        }
    }

    @PreDestroy
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
            session = null;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** Convenience API for glitch flagging. */
    public boolean hasGlitches(String imagePath) {
        if (!available) return false;
        Mat mat = Imgcodecs.imread(Paths.get(imagePath).toAbsolutePath().toString());
        if (mat == null || mat.empty()) return false;
        try {
            return hasGlitches(mat);
        } finally {
            mat.release();
        }
    }

    /** Детекция по кадру BGR (уже загруженному в память). */
    public boolean hasGlitches(Mat bgr) {
        return detect(bgr).isGlitches();
    }

    /** Runs inference for the configured target classes and returns presence flags. */
    public Detections detect(String imagePath) {
        if (!available) return new Detections(false, false, false);
        Mat mat = Imgcodecs.imread(Paths.get(imagePath).toAbsolutePath().toString());
        if (mat == null || mat.empty()) return new Detections(false, false, false);
        try {
            return detect(mat);
        } finally {
            mat.release();
        }
    }

    /** Runs inference for the configured target classes and returns presence flags. */
    public Detections detect(Mat bgr) {
        if (!available || bgr == null || bgr.empty()) return new Detections(false, false, false);
        try {
            float[] inputData = preprocessLetterbox(bgr);
            long[] shape = new long[]{1, CHANNELS, inputHeight, inputWidth};
            FloatBuffer floatBuffer = FloatBuffer.wrap(inputData);
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, floatBuffer, shape)) {
                try (OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {
                    OnnxTensor output = (OnnxTensor) result.get(0);
                    float[][] detections = getDetectionsMatrix(output);
                    return evaluateDetections(detections, bgr.rows(), bgr.cols());
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "YOLO inference failed", e);
            return new Detections(false, false, false);
        }
    }

    private float[] preprocessLetterbox(Mat bgr) {
        int h = bgr.rows();
        int w = bgr.cols();
        double scale = Math.min((double) inputHeight / h, (double) inputWidth / w);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);
        Mat resized = new Mat();
        Imgproc.resize(bgr, resized, new Size(newW, newH));
        int padW = (inputWidth - newW) / 2;
        int padH = (inputHeight - newH) / 2;
        Mat padded = new Mat(inputHeight, inputWidth, bgr.type());
        padded.setTo(LETTERBOX_COLOR);
        resized.copyTo(padded.submat(padH, padH + newH, padW, padW + newW));
        resized.release();

        float[] data = new float[CHANNELS * inputHeight * inputWidth];
        int idx = 0;
        for (int c = 0; c < CHANNELS; c++) {
            for (int y = 0; y < inputHeight; y++) {
                for (int x = 0; x < inputWidth; x++) {
                    double[] v = padded.get(y, x);
                    data[idx++] = (v != null && v.length >= 3) ? (float) (v[2 - c] / 255.0) : 0f;
                }
            }
        }
        padded.release();
        return data;
    }

    private float[][] getDetectionsMatrix(OnnxTensor output) throws Exception {
        Object val = output.getValue();
        long[] shape = ((TensorInfo) output.getInfo()).getShape();
        int rows = (int) shape[1];
        int cols = (int) shape[2];
        float[][] out = new float[rows][cols];
        if (val instanceof float[][][]) {
            float[][][] arr = (float[][][]) val;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    out[r][c] = arr[0][r][c];
        } else if (val instanceof float[]) {
            float[] flat = (float[]) val;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    out[r][c] = flat[r * cols + c];
        }
        return out;
    }

    private static final String LABEL_SCREEN = "screen";
    private static final String LABEL_GLITCHES = "glitches";
    private static final String LABEL_DEAD_PIXELS = "dead-pixels-block";

    private Detections evaluateDetections(float[][] out, int imgHeight, int imgWidth) {
        int numClasses = out.length - 4;
        int numBoxes = out[0].length;
        double gain = Math.min((double) inputHeight / imgHeight, (double) inputWidth / imgWidth);
        int padX = (inputWidth - (int) (imgWidth * gain)) / 2;
        int padY = (inputHeight - (int) (imgHeight * gain)) / 2;
        int glitchesId = settings.getGlitchesClassId();
        int screenId = settings.getScreenClassId();
        int deadBlockId = settings.getDeadPixelsBlockClassId();

        boolean hasGlitches = false;
        boolean hasScreen = false;
        boolean hasDeadBlock = false;
        List<DetectionBox> boxes = new ArrayList<>();

        for (int i = 0; i < numBoxes; i++) {
            int bestClass = -1;
            float bestScore = -1f;
            for (int c = 0; c < numClasses; c++) {
                float s = out[4 + c][i];
                if (s > bestScore) {
                    bestScore = s;
                    bestClass = c;
                }
            }

            double thres = thresholdForClass(bestClass);
            if (bestScore < thres) continue;
            if (bestClass != glitchesId && bestClass != screenId && bestClass != deadBlockId) continue;

            float cx = out[0][i] - padX;
            float cy = out[1][i] - padY;
            float bw = out[2][i];
            float bh = out[3][i];
            double x1 = (cx - bw / 2) / gain;
            double y1 = (cy - bh / 2) / gain;
            double x2 = (cx + bw / 2) / gain;
            double y2 = (cy + bh / 2) / gain;

            if (!(x1 < imgWidth && x2 > 0 && y1 < imgHeight && y2 > 0)) continue;

            String label = bestClass == glitchesId ? LABEL_GLITCHES : (bestClass == screenId ? LABEL_SCREEN : LABEL_DEAD_PIXELS);
            double x = Math.max(0, Math.min(x1, imgWidth - 1));
            double y = Math.max(0, Math.min(y1, imgHeight - 1));
            double w = Math.max(1, Math.min(x2 - x1, imgWidth - x));
            double h = Math.max(1, Math.min(y2 - y1, imgHeight - y));
            boxes.add(new DetectionBox(label, x, y, w, h, bestScore));

            if (bestClass == glitchesId) hasGlitches = true;
            if (bestClass == screenId) hasScreen = true;
            if (bestClass == deadBlockId) hasDeadBlock = true;
        }
        return new Detections(hasScreen, hasGlitches, hasDeadBlock, boxes);
    }

    private double thresholdForClass(int classId) {
        double fallback = settings.getConfThreshold();
        if (classId == settings.getGlitchesClassId()) {
            return settings.getGlitchesConfThreshold() > 0 ? settings.getGlitchesConfThreshold() : fallback;
        }
        if (classId == settings.getScreenClassId()) {
            return settings.getScreenConfThreshold() > 0 ? settings.getScreenConfThreshold() : fallback;
        }
        if (classId == settings.getDeadPixelsBlockClassId()) {
            return settings.getDeadPixelsBlockConfThreshold() > 0 ? settings.getDeadPixelsBlockConfThreshold() : fallback;
        }
        return fallback;
    }
}
