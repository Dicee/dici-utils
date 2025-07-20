package com.dici.insights.reporting;

import com.dici.exception.MultiException;

import java.util.List;
import java.util.function.BiConsumer;

public class Reporting {
    public static <METADATA, DATA> void reportAll(
            String description,
            METADATA metadata,
            DATA data,
            List<? extends BiConsumer<METADATA, DATA>> reporters
    ) {
        MultiException multiException = new MultiException(10);
        for (BiConsumer<METADATA, DATA> reporter : reporters) {
            try {
                reporter.accept(metadata, data);
            } catch (Exception e) {
                multiException.add(e);
            }
        }
        multiException.throwIfNotEmpty();
    }
}
