package com.dici.image.colorTransfomrations;

import java.util.function.Function;

import javafx.scene.paint.Color;

public interface ColorTransformation extends Function<Color, Color> {
    public static ColorTransformation identity() { return color -> color; }
}
