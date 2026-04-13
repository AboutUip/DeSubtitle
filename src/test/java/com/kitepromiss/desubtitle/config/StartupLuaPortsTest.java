package com.kitepromiss.desubtitle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StartupLuaPortsTest {

    @Test
    void portsLuaMapsToSpringKeys() {
        Path path = StartupLuaPorts.PORTS_LUA_RELATIVE.toAbsolutePath();
        assertTrue(Files.isRegularFile(path), "测试须在模块根目录执行（存在 config/lua/ports.lua）");

        Map<String, Object> props = StartupLuaPorts.loadDefaultPropertiesFromLua();
        assertEquals("8080", props.get("server.port"));
        assertEquals("5173", props.get("desubtitle.frontend.port"));
    }
}
