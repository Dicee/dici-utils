package com.dici.image.tools;

import static com.dici.image.colorTransformations.ColorTransformation.identity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;

import com.dici.image.BinaryThresholder;
import com.dici.image.Thresholder;
import com.dici.image.colorTransformations.TransparentTransformation;

public class Thresholding {
    public static void main(String[] args) throws IOException {
        // replace with appropriate path to the input image
        String inputPath = "C:/Users/Dici/Desktop/tiles4.png";
        // replace with appropriate thresholder
//        Thresholder thresholder = new BinaryThresholder(Color.color(0.5, 0.5, 0.5), x -> Color.LIGHTSLATEGRAY, identity());
        Thresholder thresholder = new BinaryThresholder(Color.BURLYWOOD.brighter(), identity(), new TransparentTransformation());
        
        BufferedImage bufferedImage = ImageIO.read(new File(inputPath));
        WritableImage outputImage   = SwingFXUtils.toFXImage(bufferedImage, null);
        
        PixelReader   pixelReader   = outputImage.getPixelReader();
        PixelWriter   pixelWriter   = outputImage.getPixelWriter();
        
        for (int x = 0; x < bufferedImage.getWidth(); x++)
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                Color newColor = thresholder.process(pixelReader.getColor(x, y));
                pixelWriter.setColor(x, y, newColor);
            }

        // replace with appropriate path to the output image
        String outputPath = "C:/Users/Dici/Desktop/tiles3.png";
//        String outputPath = "C:/JavaWork/Scala/algorithmicProblems/src/miscellaneous/chess/guifx/resources/tiles.png";
        ImageIO.write(SwingFXUtils.fromFXImage(outputImage, null), "png", new File(outputPath));
    }
}
