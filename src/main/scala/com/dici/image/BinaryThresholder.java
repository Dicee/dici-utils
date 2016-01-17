package com.dici.image;

import static com.dici.check.Check.notNull;

import com.dici.image.colorTransfomrations.ColorTransformation;

import javafx.scene.paint.Color;

public class BinaryThresholder implements Thresholder {
    private final Color threshold;
    private ColorTransformation belowThreshold;
    private ColorTransformation aboveThreshold;

    public BinaryThresholder(Color threshold, ColorTransformation belowThreshold, ColorTransformation aboveThreshold) {
        this.threshold      = notNull(threshold     );
        this.belowThreshold = notNull(belowThreshold);
        this.aboveThreshold = notNull(aboveThreshold);
    }

    @Override
    public boolean test(Color color) {
        return color.getRed() < threshold.getRed() && color.getGreen() < threshold.getGreen() && color.getBlue() < threshold.getBlue();
    }

    @Override
    public Color handleBelowThreshold(Color color) {
        return belowThreshold.apply(color);
    }

    @Override
    public Color handleAboveThreshold(Color color) {
        return aboveThreshold.apply(color);
    }
}