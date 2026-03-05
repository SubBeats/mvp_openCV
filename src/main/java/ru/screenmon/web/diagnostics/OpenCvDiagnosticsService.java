package ru.screenmon.web.diagnostics;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import ru.screenmon.config.CvSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис диагностики OpenCV: разбор одного изображения (средняя яркость, дисперсия, флаг «чёрный экран»).
 * Используется эндпоинтами /api/demo/*.
 */
@Service
public class OpenCvDiagnosticsService {

    private final CvSettings cv;

    public OpenCvDiagnosticsService(CvSettings cv) {
        this.cv = cv;
    }

    public Map<String, Object> analyzeImage(Path imagePath) {
        Mat mat = Imgcodecs.imread(imagePath.toAbsolutePath().toString());
        if (mat == null || mat.empty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "Could not load image");
            return err;
        }
        Mat gray = new Mat();
        try {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfDouble meanMat = new MatOfDouble();
            MatOfDouble stdMat = new MatOfDouble();
            Core.meanStdDev(gray, meanMat, stdMat);
            double meanY = meanMat.get(0, 0)[0];
            double std = stdMat.get(0, 0)[0];
            double var = std * std;
            boolean isBlack = meanY < cv.getBlackMeanThreshold() && var < cv.getBlackVarThreshold();
            Map<String, Object> out = new HashMap<>();
            out.put("ok", true);
            out.put("width", mat.cols());
            out.put("height", mat.rows());
            out.put("meanY", Math.round(meanY * 100) / 100.0);
            out.put("std", Math.round(std * 100) / 100.0);
            out.put("var", Math.round(var * 100) / 100.0);
            out.put("isBlack", isBlack);
            out.put("cvUsed", true);
            return out;
        } finally {
            mat.release();
            gray.release();
        }
    }

    public Map<String, Object> analyzeSampleFrame() {
        Mat mat = new Mat(100, 100, org.opencv.core.CvType.CV_8UC3, new Scalar(0, 0, 0));
        try {
            Path temp = Files.createTempFile("cv_sample_", ".jpg");
            try {
                Imgcodecs.imwrite(temp.toString(), mat);
                return analyzeImage(temp);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", e.getMessage());
            return err;
        } finally {
            mat.release();
        }
    }
}

