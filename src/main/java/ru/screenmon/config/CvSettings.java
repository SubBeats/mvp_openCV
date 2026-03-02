package ru.screenmon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки OpenCV-эвристик.
 *
 * <p>Все свойства задаются в {@code application.yml} с префиксом {@code app.cv.*}.</p>
 *
 * <p><b>Основные параметры:</b></p>
 * <ul>
 *   <li><b>Чёрный экран:</b> {@code black-mean-threshold}, {@code black-var-threshold} — пороги по средней яркости и дисперсии; ниже — кадр считаем «чёрным».</li>
 *   <li><b>Фриз:</b> {@code freeze-phash-threshold} — макс. расстояние Хэмминга между pHash кадров, чтобы считать их одинаковыми; {@code freeze-frames-window} — размер окна по предыдущим кадрам; {@code freeze-frames-match} — сколько совпадений нужно для флага фриза.</li>
 *   <li>Битые блоки детектируются только нейросетью (YOLO), см. {@code app.yolo.*}.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.cv")
public class CvSettings {

    /** Порог средней яркости (0..255): кадр «чёрный», если средняя яркость ниже этого значения. */
    private double blackMeanThreshold = 10;

    /** Порог дисперсии яркости: кадр «чёрный», если контраст (дисперсия) ниже — картинка почти однотонная. */
    private double blackVarThreshold = 5;

    /** Зарезервировано под детектор «затемнения». */
    private double dimBrightnessThreshold = 80;

    /** Зарезервировано под логику «N чёрных кадров подряд». */
    private int blackFramesInRow = 2;

    /** Макс. расстояние Хэмминга между pHash двух кадров, чтобы считать их «одинаковыми» (фриз). Чем меньше — тем строже. */
    private int freezePhashThreshold = 6;

    /** Сколько кадров из окна должны совпасть по pHash с текущим, чтобы выставить фриз. */
    private int freezeFramesMatch = 2;

    /** Размер окна (кол-во предыдущих кадров) для проверки фриза. */
    private int freezeFramesWindow = 3;

    /** Зарезервировано под детектор полос/бандинга. */
    private double bandsDeviationThreshold = 0.25;

    /** Зарезервировано под детектор сдвига цвета. */
    private double colorDeltaEThreshold = 12;

    public double getBlackMeanThreshold() { return blackMeanThreshold; }
    public void setBlackMeanThreshold(double blackMeanThreshold) { this.blackMeanThreshold = blackMeanThreshold; }
    public double getBlackVarThreshold() { return blackVarThreshold; }
    public void setBlackVarThreshold(double blackVarThreshold) { this.blackVarThreshold = blackVarThreshold; }
    public double getDimBrightnessThreshold() { return dimBrightnessThreshold; }
    public void setDimBrightnessThreshold(double dimBrightnessThreshold) { this.dimBrightnessThreshold = dimBrightnessThreshold; }
    public int getBlackFramesInRow() { return blackFramesInRow; }
    public void setBlackFramesInRow(int blackFramesInRow) { this.blackFramesInRow = blackFramesInRow; }
    public int getFreezePhashThreshold() { return freezePhashThreshold; }
    public void setFreezePhashThreshold(int freezePhashThreshold) { this.freezePhashThreshold = freezePhashThreshold; }
    public int getFreezeFramesMatch() { return freezeFramesMatch; }
    public void setFreezeFramesMatch(int freezeFramesMatch) { this.freezeFramesMatch = freezeFramesMatch; }
    public int getFreezeFramesWindow() { return freezeFramesWindow; }
    public void setFreezeFramesWindow(int freezeFramesWindow) { this.freezeFramesWindow = freezeFramesWindow; }
    public double getBandsDeviationThreshold() { return bandsDeviationThreshold; }
    public void setBandsDeviationThreshold(double bandsDeviationThreshold) { this.bandsDeviationThreshold = bandsDeviationThreshold; }
    public double getColorDeltaEThreshold() { return colorDeltaEThreshold; }
    public void setColorDeltaEThreshold(double colorDeltaEThreshold) { this.colorDeltaEThreshold = colorDeltaEThreshold; }
}

