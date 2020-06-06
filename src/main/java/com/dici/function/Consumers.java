package com.dici.function;

import java.util.function.Consumer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Consumers {
    public static <T> Consumer<T> noop() {
        return ignored -> {};
    }
}
