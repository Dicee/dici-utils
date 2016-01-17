package com.dici.image;

import javafx.scene.paint.Color;

public class ColorUtils {
    public static int toRGB(Color color) {
        int r = ((int) color.getRed  () * 255);
        int g = ((int) color.getGreen() * 255);
        int b = ((int) color.getBlue () * 255);
        return (r << 16) + (g << 8) + b;
    }
    
    private ColorUtils() { }
}
