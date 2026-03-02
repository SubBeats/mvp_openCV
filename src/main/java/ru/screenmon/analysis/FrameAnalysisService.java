package ru.screenmon.analysis;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import ru.screenmon.config.CvSettings;
import ru.screenmon.config.YoloSettings;
import ru.screenmon.storage.FrameAnalysis;
import ru.screenmon.storage.FrameAnalysisRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Сервис анализа одного кадра и сохранения результата в БД.
 *
 * <p><b>Назначение:</b> по пути к сохранённому кадру вычисляет признаки (чёрный экран, фриз, битые блоки, глитчи/экран по YOLO) и записывает их в {@link ru.screenmon.storage.FrameAnalysis}.</p>
 *
 * <p><b>Основные шаги:</b></p>
 * <ul>
 *   <li>Чёрный экран — по средней яркости и дисперсии (пороги в {@link ru.screenmon.config.CvSettings}).</li>
 *   <li>Фриз — по совпадению pHash с предыдущими кадрами (окно и порог в CvSettings).</li>
 *   <li>Глитчи, экран и битые блоки — только из нейросети (YOLO/Roboflow). OpenCV к ним не подключается. Пороги и номера классов в {@link ru.screenmon.config.YoloSettings}. Устойчивость по N кадрам — {@code glitches-frames-persist}, {@code dead-pixels-block-frames-persist}.</li>
 * </ul>
 *
 * <p>Параметры CV: {@code app.cv.*}; параметры YOLO: {@code app.yolo.*}.</p>
 */
@Service
public class FrameAnalysisService {

    private static final int HASH_SIZE = 8;

    private final CvSettings cv;
    private final YoloSettings yoloSettings;
    private final FrameAnalysisRepository repository;
    private final RoboflowDetectionService roboflowDetection;
    private final YoloDetectionService yoloDetection;

    public FrameAnalysisService(CvSettings cv, YoloSettings yoloSettings, FrameAnalysisRepository repository,
                               RoboflowDetectionService roboflowDetection, YoloDetectionService yoloDetection) {
        this.cv = cv;
        this.yoloSettings = yoloSettings;
        this.repository = repository;
        this.roboflowDetection = roboflowDetection;
        this.yoloDetection = yoloDetection;
    }

    public AnalysisResult analyzeAndSave(Long screenId, String framePath) {
        Path path = Paths.get(framePath);
        Mat mat = Imgcodecs.imread(path.toAbsolutePath().toString());
        if (mat == null || mat.empty()) {
            return new AnalysisResult(null, Collections.emptyList());
        }
        Mat gray = new Mat();
        try {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            GrayStats grayStats = computeGrayStats(gray);
            boolean isBlack = isBlack(grayStats);
            String phash = computeAverageHash(gray);
            List<FrameAnalysis> previous = repository.findTop20ByScreenIdOrderByTsDesc(screenId);
            boolean isFreeze = checkFreeze(previous, phash);

            YoloDetectionService.Detections yolo = roboflowDetection.isAvailable()
                    ? roboflowDetection.detect(mat)
                    : (yoloDetection.isAvailable() ? yoloDetection.detect(mat) : null);
            List<DetectionBox> boxes = yolo != null ? yolo.getBoxes() : Collections.emptyList();
            boolean yoloScreen = yolo != null && yolo.isScreen();
            boolean yoloGlitchesRaw = yolo != null && yolo.isGlitches();
            boolean yoloDeadPixelsBlockRaw = yolo != null && yolo.isDeadPixelsBlock();
            boolean yoloGlitches = applyConsecutivePersist(previous, yoloGlitchesRaw, yoloSettings.getGlitchesFramesPersist(), FrameAnalysis::getYoloGlitchesDetected);
            boolean yoloDeadPixelsBlock = applyConsecutivePersist(previous, yoloDeadPixelsBlockRaw, yoloSettings.getDeadPixelsBlockFramesPersist(), FrameAnalysis::getYoloDeadPixelsBlockDetected);

            FrameAnalysis entity = new FrameAnalysis();
            entity.setScreenId(screenId);
            entity.setTs(Instant.now());
            entity.setFramePath(framePath);
            entity.setMeanY(grayStats.meanY);
            entity.setVarY(grayStats.varY);
            entity.setPhash(phash);
            entity.setIsBlack(isBlack);
            entity.setIsDim(false);
            entity.setIsFreeze(isFreeze);
            entity.setDeadPixelsDetected(yoloDeadPixelsBlock);
            entity.setYoloGlitchesDetected(yoloGlitches);
            entity.setYoloScreenDetected(yoloScreen);
            entity.setYoloDeadPixelsBlockDetected(yoloDeadPixelsBlock);
            entity.setCreatedAt(Instant.now());
            FrameAnalysis saved = repository.save(entity);
            return new AnalysisResult(saved, boxes);
        } finally {
            mat.release();
            gray.release();
        }
    }

    private static final class GrayStats {
        private final double meanY;
        private final double varY;

        private GrayStats(double meanY, double varY) {
            this.meanY = meanY;
            this.varY = varY;
        }
    }

    private GrayStats computeGrayStats(Mat gray) {
        MatOfDouble meanMat = new MatOfDouble();
        MatOfDouble stdMat = new MatOfDouble();
        try {
            Core.meanStdDev(gray, meanMat, stdMat);
            double meanY = meanMat.get(0, 0)[0];
            double std = stdMat.get(0, 0)[0];
            return new GrayStats(meanY, std * std);
        } finally {
            meanMat.release();
            stdMat.release();
        }
    }

    private boolean isBlack(GrayStats stats) {
        return stats.meanY < cv.getBlackMeanThreshold() && stats.varY < cv.getBlackVarThreshold();
    }

    private String computeAverageHash(Mat gray) {
        Mat small = new Mat();
        Imgproc.resize(gray, small, new Size(HASH_SIZE, HASH_SIZE));
        MatOfDouble meanMat = new MatOfDouble();
        MatOfDouble stdMat = new MatOfDouble();
        Core.meanStdDev(small, meanMat, stdMat);
        double mean = meanMat.get(0, 0)[0];
        StringBuilder sb = new StringBuilder(64);
        for (int y = 0; y < HASH_SIZE; y++) {
            for (int x = 0; x < HASH_SIZE; x++) {
                double[] v = small.get(y, x);
                sb.append(v != null && v[0] >= mean ? '1' : '0');
            }
        }
        small.release();
        return sb.toString();
    }

    private static int hammingDistance(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return Integer.MAX_VALUE;
        int d = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) d++;
        }
        return d;
    }

    /**
     * Детектор фриза: текущий pHash сравнивается с предыдущими кадрами; нужно нужное число совпадений в окне.
     */
    private boolean checkFreeze(List<FrameAnalysis> previous, String currentPhash) {
        int window = Math.max(1, cv.getFreezeFramesWindow());
        int needed = Math.max(1, cv.getFreezeFramesMatch());
        int threshold = cv.getFreezePhashThreshold();
        int limit = Math.min(window, previous.size());
        if (limit < needed) return false;

        int matches = 0;
        for (int i = 0; i < limit; i++) {
            String prevHash = previous.get(i).getPhash();
            if (prevHash != null && hammingDistance(currentPhash, prevHash) <= threshold) {
                matches++;
            }
        }
        return matches >= needed;
    }

    /**
     * Флаг выставляется только если на текущем кадре детекция есть и на предыдущих (persist - 1) кадрах подряд тоже была.
     * persist == 1 — без проверки подряд (текущий кадр решает).
     */
    private boolean applyConsecutivePersist(List<FrameAnalysis> previous, boolean currentResult, int persist, Function<FrameAnalysis, Boolean> getter) {
        if (persist <= 1) return currentResult;
        if (!currentResult) return false;
        int needPrev = persist - 1;
        if (previous.size() < needPrev) return false;
        for (int i = 0; i < needPrev; i++) {
            if (!Boolean.TRUE.equals(getter.apply(previous.get(i)))) return false;
        }
        return true;
    }

}
