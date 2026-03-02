package ru.screenmon.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.screenmon.analysis.AnalysisResult;
import ru.screenmon.analysis.FrameAnalysisService;
import ru.screenmon.storage.FrameAnalysis;

import java.time.Instant;

@Service
public class FrameUploadHandler {

    private final UploadService uploadService;
    private final FrameAnalysisService analysisService;

    public FrameUploadHandler(UploadService uploadService, FrameAnalysisService analysisService) {
        this.uploadService = uploadService;
        this.analysisService = analysisService;
    }

    public UploadResult saveAndAnalyze(Long screenId, Instant timestamp, MultipartFile file) throws Exception {
        String savedPath = uploadService.save(screenId, timestamp, file);
        AnalysisResult result = analysisService.analyzeAndSave(screenId, savedPath);
        FrameAnalysis analysis = result != null ? result.getAnalysis() : null;

        UploadResult.AnalysisSummary summary = null;
        if (analysis != null) {
            summary = new UploadResult.AnalysisSummary(
                Boolean.TRUE.equals(analysis.getIsBlack()),
                Boolean.TRUE.equals(analysis.getIsDim()),
                Boolean.TRUE.equals(analysis.getIsFreeze()),
                Boolean.TRUE.equals(analysis.getDeadPixelsDetected()),
                Boolean.TRUE.equals(analysis.getYoloGlitchesDetected()),
                Boolean.TRUE.equals(analysis.getYoloScreenDetected()),
                Boolean.TRUE.equals(analysis.getYoloDeadPixelsBlockDetected())
            );
        }
        return new UploadResult(screenId, timestamp, savedPath, summary, result != null ? result.getBoxes() : null);
    }
}
