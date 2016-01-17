package com.dici.image.colorTransfomrations;

import javafx.scene.paint.Color;

public class TransparentTransformation implements ColorTransformation {
    @Override public Color apply(Color color) { return new Color(color.getRed(), color.getGreen(), color.getBlue(), 0); }
}
