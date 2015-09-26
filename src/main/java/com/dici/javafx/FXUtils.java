package com.dici.javafx;

import javafx.scene.control.TreeItem;

public class FXUtils {
    public static <T> int getDepth(TreeItem<T> treeItem) {
        int depth = 0;
        while (treeItem.getParent() != null) depth++;
        return depth;
    }
    
    private FXUtils() { }
}
