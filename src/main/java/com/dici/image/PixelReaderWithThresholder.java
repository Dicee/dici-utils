package com.dici.image;

import static com.dici.check.Check.notNull;
import static com.dici.image.ColorUtils.toRGB;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.dici.check.Check;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;

public class PixelReaderWithThresholder implements PixelReader {
    private final Thresholder thresholder;
    private final PixelReader wrapped;
    
    public PixelReaderWithThresholder(PixelReader wrapped, Thresholder thresholder) { 
        this.wrapped     = notNull(wrapped    );
        this.thresholder = notNull(thresholder); 
    }
    
    @Override
    public int getArgb(int x, int y) { return toRGB(getColor(x, y)); }

    @Override
    public Color getColor(int x, int y) { return thresholder.process(wrapped.getColor(x, y)); }

    @Override
    public PixelFormat<?> getPixelFormat() { return wrapped.getPixelFormat(); }

    @Override
    public <T extends Buffer> void getPixels(int x, int y, int w, int h, WritablePixelFormat<T> pixelFormat, T buffer, int scanlineStride) {
        
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelFormat, byte[] buffer, int offset, int scanlineStride) {
        
    }

    @Override
    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelFormat, int[] buffer, int offset, int scanlineStride) {
        
    }
}
