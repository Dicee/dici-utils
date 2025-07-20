package com.dici.json;

import com.dici.collection.CollectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collection;

public class Json {
    private static final ThreadLocal<ObjectMapper> OBJECT_MAPPER = ThreadLocal.withInitial(Json::createDefaultMapper);

    /// Default [JsonSerDe] instance using basic configuration for the object mapper. Add more static instances as you see fit,
    /// but only if they're broadly shared, otherwise they can be injected directly where they're needed. This pattern allows
    /// using a common configuration without dependency injection, as if using a static method, while allowing different
    /// configurations to be used through different static instances.
    public static final JsonSerDe DEFAULT = JsonSerDe.of(OBJECT_MAPPER);

    public static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule(), new Jdk8Module(), new GuavaModule());
        return mapper;
    }

    public ArrayNode arrayNode(Collection<? extends JsonNode> nodes) {
        var arrayNode = arrayNode();
        arrayNode.addAll(nodes);
        return arrayNode;
    }

    public ArrayNode arrayNode() {
        return OBJECT_MAPPER.get().createArrayNode();
    }

    public ObjectNode objectNode() {
        return OBJECT_MAPPER.get().createObjectNode();
    }
}
