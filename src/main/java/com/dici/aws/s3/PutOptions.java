package com.dici.aws.s3;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

import java.util.Map;

@Value
@With
@RequiredArgsConstructor
public class PutOptions {
    @NonNull private final Map<String, String> metadata;

    public PutOptions() {
        this(Map.of());
    }
}
