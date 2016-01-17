package com.dici.image;

import java.util.function.Predicate;

import javafx.scene.paint.Color;

public interface Thresholder extends Predicate<Color> {
    default Color handleBelowThreshold(Color color) { return color; }
    default Color handleAboveThreshold(Color color) { return color; }
    default Color process(Color color) { return test(color) ? handleBelowThreshold(color) : handleAboveThreshold(color); }
}
