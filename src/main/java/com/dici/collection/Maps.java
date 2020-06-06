package com.dici.collection;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import java.util.Map;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Maps {
    public static <K, V, VO> Map<K, VO> mapValues(Map<K, V> map, Function<? super V, ? extends VO> valueMapper) {
        return map.entrySet().stream().collect(toImmutableMap(
                Map.Entry::getKey,
                entry -> valueMapper.apply(entry.getValue())
        ));
    }
}
