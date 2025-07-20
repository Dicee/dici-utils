package com.dici.json;

import com.dici.exceptions.ExceptionUtils;
import com.dici.exceptions.ExceptionUtils.ThrowingSupplier;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.errorprone.annotations.ThreadSafe;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/// This class makes it easier to use Jackson ser/de features by providing some higher-level shorthands with consistent
/// error handling and no pesky checked exceptions. Instances of this class will often be accessed statically via [Json],
/// hence the use of a thread-local [ObjectMapper] (I've seen bugs stemming from the parallel use of the same object mapper
/// in the past).
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonSerDe {
    public static JsonSerDe of(Supplier<ObjectMapper> supplier) {
        return JsonSerDe.of(ThreadLocal.withInitial(supplier));
    }

    public static JsonSerDe of(ThreadLocal<ObjectMapper> threadLocal) {
        return new JsonSerDe(threadLocal::get);
    }

    @NonNull private final Supplier<ObjectMapper> mapper;

    public ObjectNode readJsonObject(String json) {
        return (ObjectNode) readJsonTree(json);
    }

    public ObjectNode readJsonObject(InputStream inputStream) {
        return (ObjectNode) readJsonTree(inputStream);
    }

    public JsonNode readJsonTree(String json) {
        return tryGet(() -> mapper.get().readTree(json), "Failed to parse string as JSON tree");
    }

    public JsonNode readJsonTree(InputStream inputStream) {
        return tryGet(() -> mapper.get().readTree(inputStream), "Failed to parse input stream as JSON tree");
    }

    public <T> String toJsonString(T t) {
        return toJsonString(t, false);
    }

    public <T> String toJsonString(T t, boolean prettyPrint) {
        ObjectWriter writer = prettyPrint ? mapper.get().writerWithDefaultPrettyPrinter() : mapper.get().writer();
        return tryGet(() -> writer.writeValueAsString(t), "Failed to serialize %s", t);
    }

    public <T> T readObject(String json, Class<T> clazz) {
        return tryGet(() -> mapper.get().readValue(json, clazz), "Failed to deserialize %s stream as %s", json, clazz);
    }

    public <T> T readObject(TreeNode node, Class<T> clazz) {
        return tryGet(() -> mapper.get().treeToValue(node, clazz), "Failed to deserialize %s as %s", node, clazz);
    }

    public <T> T readObject(File file, Class<T> clazz) {
        return tryGet(() -> mapper.get().readValue(file, clazz), "Failed to deserialize %s stream as %s", file.getAbsolutePath(), clazz);
    }

    public <T> T readObject(InputStream inputStream, Class<T> clazz) {
        return tryGet(() -> mapper.get().readValue(inputStream, clazz), "Failed to deserialize input stream as %s", clazz);
    }

    public <T> T readObject(String json, TypeReference<T> typeReference) {
        return tryGet(() -> mapper.get().readValue(json, typeReference), "Failed to deserialize %s as %s", json, typeReference);
    }

    private static <T> T tryGet(ThrowingSupplier<T, ? extends IOException> supplier, String errorMsgFormat, Object... args) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedJsonParseException(errorMsgFormat.formatted(args), e);
        }
    }
}
