package com.kitepromiss.desubtitle.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 读写 {@code config/json} 下的 JSON 配置；用于可持久化修改的运行时需求。
 */
public final class JsonConfigLoader {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsonConfigLoader() {}

    /**
     * 读取 JSON 为树，便于查询与就地修改后写回。
     */
    public static JsonNode loadTree(Path jsonFile) throws IOException {
        return MAPPER.readTree(Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8));
    }

    /**
     * 将树写回文件（UTF-8，带缩进）。
     */
    public static void writeTree(Path jsonFile, JsonNode root) throws IOException {
        Files.createDirectories(jsonFile.getParent());
        try (var writer = Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8)) {
            MAPPER.writeValue(writer, root);
        }
    }
}
