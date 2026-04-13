package com.kitepromiss.desubtitle.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * 执行 {@code config/lua} 下只读参考用 Lua 并取根返回值。脚本须以 {@code return { ... }} 形式导出配置表。
 */
public final class LuaConfigLoader {

    private LuaConfigLoader() {}

    /**
     * 加载并执行脚本，返回 chunk 的返回值（通常为 {@link LuaTable}）。
     */
    public static LuaValue loadReturningRoot(Path luaFile) throws IOException {
        Globals globals = JsePlatform.standardGlobals();
        try (InputStream in = Files.newInputStream(luaFile)) {
            LuaValue chunk = globals.load(in, luaFile.toString(), "t", globals);
            return chunk.call();
        }
    }

    /**
     * 与 {@link #loadReturningRoot(Path)} 相同，且要求根值为 table。
     *
     * @throws LuaError 根值非 table
     */
    public static LuaTable loadAsTable(Path luaFile) throws IOException {
        LuaValue root = loadReturningRoot(luaFile);
        if (!root.istable()) {
            throw new LuaError("配置根须为 table，实际为 " + root.typename());
        }
        return root.checktable();
    }
}
