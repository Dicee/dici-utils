package com.dici.javafx;

import static com.dici.check.Check.notBlank;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXUtils {
    public static <T> int getDepth(TreeItem<T> treeItem) {
        int depth = 0;
        while (treeItem.getParent() != null) depth++;
        return depth;
    }
    
    public static ImageView imageViewFromResource(String localPath, Class<?> clazz) {
        return new ImageView(getImageResource(localPath, clazz));
    }
    
    public static Image getImageResource(String localPath, Class<?> clazz) {
        return new Image(clazz.getResourceAsStream(notBlank(localPath)));
    }
    
    private FXUtils() { }
}
