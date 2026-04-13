package com.kitepromiss.desubtitle.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * 从 {@code config/lua/ports.lua} 读取端口并映射为 Spring {@code SpringApplication#setDefaultProperties(Map<String, Object>)} 的条目。
 * 默认属性优先级低于 {@code config/yml/application.yml}（经 {@code spring.config.import} 加载）与命令行参数。
 */
public final class StartupLuaPorts {

    /** 仓库内相对路径，解析于 JVM 工作目录。 */
    public static final Path PORTS_LUA_RELATIVE = Path.of("config", "lua", "ports.lua");

    private static final String KEY_BACKEND = "backend_port";
    private static final String KEY_FRONTEND = "frontend_port";

    private StartupLuaPorts() {}

    /**
     * 若存在 {@value #PORTS_LUA_RELATIVE} 则解析；否则返回空映射（沿用 Spring 与 properties 默认）。
     */
    public static Map<String, Object> loadDefaultPropertiesFromLua() {
        Path absolute = PORTS_LUA_RELATIVE.toAbsolutePath();
        if (!Files.isRegularFile(absolute)) {
            return Map.of();
        }
        try {
            LuaTable table = LuaConfigLoader.loadAsTable(absolute);
            Map<String, Object> map = new LinkedHashMap<>();
            putPort(table, KEY_BACKEND, "server.port", map);
            putPort(table, KEY_FRONTEND, "desubtitle.frontend.port", map);
            return Map.copyOf(map);
        } catch (IOException e) {
            throw new UncheckedIOException("读取 ports.lua 失败: " + absolute, e);
        }
    }

    private static void putPort(LuaTable table, String luaKey, String springKey, Map<String, Object> target) {
        LuaValue v = table.get(luaKey);
        if (v.isnil()) {
            return;
        }
        if (!v.isnumber()) {
            throw new IllegalStateException("ports.lua 中 " + luaKey + " 须为数字，实际为 " + v.typename());
        }
        int port = v.toint();
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("ports.lua 中 " + luaKey + " 须在 1–65535，实际为 " + port);
        }
        target.put(springKey, Integer.toString(port));
    }
}
