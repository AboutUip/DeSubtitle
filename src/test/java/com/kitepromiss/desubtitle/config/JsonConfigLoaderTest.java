package com.kitepromiss.desubtitle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

class JsonConfigLoaderTest {

    @Test
    void runtimeJsonHasInitializationFlag() throws Exception {
        Path path = Path.of("config/json/runtime.json").toAbsolutePath();
        assertTrue(Files.isRegularFile(path), "测试须在模块根目录执行（存在 config/json/runtime.json）");
        JsonNode root = JsonConfigLoader.loadTree(path);
        assertTrue(root.isObject());
        assertTrue(root.path("initialization_completed").isBoolean());
    }

    @Test
    void writeThenReadRoundTrip(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("t.json");
        var node = JsonNodeFactory.instance.objectNode().put("key", "value");
        JsonConfigLoader.writeTree(file, node);
        JsonNode read = JsonConfigLoader.loadTree(file);
        assertEquals("value", read.get("key").asText());
    }
}
