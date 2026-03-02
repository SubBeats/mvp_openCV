package ru.screenmon.storage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Результат анализа одного кадра: флаги (чёрный экран, фриз, битые пиксели, глитчи по YOLO и т.д.),
 * метрики (meanY, varY, pHash) и путь к сохранённому файлу кадра.
 */
@Entity
@Table(name = "frame_analysis")
public class FrameAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(nullable = false)
    private Instant ts;

    @Column(name = "frame_path")
    private String framePath;

    @Column(name = "mean_y")
    private Double meanY;

    @Column(name = "var_y")
    private Double varY;

    @Column(name = "phash", length = 64)
    private String phash;

    @Column(name = "is_black")
    private Boolean isBlack;

    @Column(name = "is_dim")
    private Boolean isDim;

    @Column(name = "is_freeze")
    private Boolean isFreeze;

    @Column(name = "dead_pixels_detected")
    private Boolean deadPixelsDetected;

    @Column(name = "yolo_glitches_detected")
    private Boolean yoloGlitchesDetected;

    @Column(name = "yolo_screen_detected")
    private Boolean yoloScreenDetected;

    @Column(name = "yolo_dead_pixels_block_detected")
    private Boolean yoloDeadPixelsBlockDetected;

    @Column(name = "created_at")
    private Instant createdAt;

    public FrameAnalysis() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }

    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }

    public String getFramePath() { return framePath; }
    public void setFramePath(String framePath) { this.framePath = framePath; }

    public Double getMeanY() { return meanY; }
    public void setMeanY(Double meanY) { this.meanY = meanY; }

    public Double getVarY() { return varY; }
    public void setVarY(Double varY) { this.varY = varY; }

    public String getPhash() { return phash; }
    public void setPhash(String phash) { this.phash = phash; }

    public Boolean getIsBlack() { return isBlack; }
    public void setIsBlack(Boolean isBlack) { this.isBlack = isBlack; }

    public Boolean getIsDim() { return isDim; }
    public void setIsDim(Boolean isDim) { this.isDim = isDim; }

    public Boolean getIsFreeze() { return isFreeze; }
    public void setIsFreeze(Boolean isFreeze) { this.isFreeze = isFreeze; }

    public Boolean getDeadPixelsDetected() { return deadPixelsDetected; }
    public void setDeadPixelsDetected(Boolean deadPixelsDetected) { this.deadPixelsDetected = deadPixelsDetected; }

    public Boolean getYoloGlitchesDetected() { return yoloGlitchesDetected; }
    public void setYoloGlitchesDetected(Boolean yoloGlitchesDetected) { this.yoloGlitchesDetected = yoloGlitchesDetected; }

    public Boolean getYoloScreenDetected() { return yoloScreenDetected; }
    public void setYoloScreenDetected(Boolean yoloScreenDetected) { this.yoloScreenDetected = yoloScreenDetected; }

    public Boolean getYoloDeadPixelsBlockDetected() { return yoloDeadPixelsBlockDetected; }
    public void setYoloDeadPixelsBlockDetected(Boolean yoloDeadPixelsBlockDetected) { this.yoloDeadPixelsBlockDetected = yoloDeadPixelsBlockDetected; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
