package com.kitepromiss.desubtitle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;

class LuaConfigLoaderTest {

    @Test
    void portsLuaLoadsAsTableWithExpectedKeys() throws Exception {
        Path path = Path.of("config/lua/ports.lua").toAbsolutePath();
        assertTrue(Files.isRegularFile(path), "测试须在模块根目录执行（存在 config/lua/ports.lua）");

        LuaTable table = LuaConfigLoader.loadAsTable(path);
        assertNotNull(table);
        assertTrue(table.get("backend_port").isnumber());
        assertEquals(8080, table.get("backend_port").toint());
    }

    @Test
    void runtimeModeLuaHasDebugModeFalse() throws Exception {
        Path path = Path.of("config/lua/runtime_mode.lua").toAbsolutePath();
        assertTrue(Files.isRegularFile(path), "测试须在模块根目录执行（存在 config/lua/runtime_mode.lua）");

        LuaTable table = LuaConfigLoader.loadAsTable(path);
        assertTrue(table.get("debug_mode").isboolean());
        assertEquals(false, table.get("debug_mode").toboolean());
    }
}
