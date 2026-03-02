package ru.screenmon.upload;

import ru.screenmon.analysis.DetectionBox;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class UploadResult {

    private final Long screenId;
    private final Instant timestamp;
    private final String savedPath;
    private final AnalysisSummary analysis;
    private final List<DetectionBox> boxes;

    public UploadResult(Long screenId, Instant timestamp, String savedPath, AnalysisSummary analysis) {
        this(screenId, timestamp, savedPath, analysis, Collections.emptyList());
    }

    public UploadResult(Long screenId, Instant timestamp, String savedPath, AnalysisSummary analysis, List<DetectionBox> boxes) {
        this.screenId = screenId;
        this.timestamp = timestamp;
        this.savedPath = savedPath;
        this.analysis = analysis;
        this.boxes = boxes != null ? boxes : Collections.emptyList();
    }

    public Long getScreenId() { return screenId; }
    public Instant getTimestamp() { return timestamp; }
    public String getSavedPath() { return savedPath; }
    public AnalysisSummary getAnalysis() { return analysis; }
    public List<DetectionBox> getBoxes() { return boxes; }

    public static class AnalysisSummary {
        private final boolean black;
        private final boolean dim;
        private final boolean freeze;
        private final boolean deadPixels;
        private final boolean yoloGlitches;
        private final boolean yoloScreen;
        private final boolean yoloDeadPixelsBlock;

        public AnalysisSummary(boolean black,
                              boolean dim,
                              boolean freeze,
                              boolean deadPixels,
                              boolean yoloGlitches,
                              boolean yoloScreen,
                              boolean yoloDeadPixelsBlock) {
            this.black = black;
            this.dim = dim;
            this.freeze = freeze;
            this.deadPixels = deadPixels;
            this.yoloGlitches = yoloGlitches;
            this.yoloScreen = yoloScreen;
            this.yoloDeadPixelsBlock = yoloDeadPixelsBlock;
        }

        public boolean isBlack() { return black; }
        public boolean isDim() { return dim; }
        public boolean isFreeze() { return freeze; }
        public boolean isDeadPixels() { return deadPixels; }
        public boolean isYoloGlitches() { return yoloGlitches; }
        public boolean isYoloScreen() { return yoloScreen; }
        public boolean isYoloDeadPixelsBlock() { return yoloDeadPixelsBlock; }
    }
}
