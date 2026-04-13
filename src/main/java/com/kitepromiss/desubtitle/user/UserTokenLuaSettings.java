package com.kitepromiss.desubtitle.user;

import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.config.LuaConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 读取 {@code config/lua/user_token.lua} 中的 {@code token_ttl_minutes}（分钟）。
 */
@Service
public class UserTokenLuaSettings {

    private static final Logger log = LoggerFactory.getLogger(UserTokenLuaSettings.class);

    /** 与 {@code user_token.lua} 中键名一致。 */
    public static final String LUA_KEY_TOKEN_TTL_MINUTES = "token_ttl_minutes";

    public static final int DEFAULT_TTL_MINUTES = 60;
    public static final int MIN_TTL_MINUTES = 1;
    /** 上限 7 天，防止配置误填过大。 */
    public static final int MAX_TTL_MINUTES = 10080;

    private final WorkspacePaths workspacePaths;

    public UserTokenLuaSettings(WorkspacePaths workspacePaths) {
        this.workspacePaths = workspacePaths;
    }

    /**
     * @return 夹在 [{@value #MIN_TTL_MINUTES}, {@value #MAX_TTL_MINUTES}] 内的分钟数；文件缺失或解析失败时返回 {@value #DEFAULT_TTL_MINUTES}
     */
    public int tokenTtlMinutes() {
        Path p = workspacePaths.userTokenLua();
        if (!Files.isRegularFile(p)) {
            return DEFAULT_TTL_MINUTES;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get(LUA_KEY_TOKEN_TTL_MINUTES);
            if (v.isnil() || !v.isnumber()) {
                return DEFAULT_TTL_MINUTES;
            }
            int n = (int) Math.round(v.todouble());
            return Math.min(MAX_TTL_MINUTES, Math.max(MIN_TTL_MINUTES, n));
        } catch (Exception e) {
            log.warn("读取 user_token.lua 失败，使用默认 TTL {} 分钟: {}", DEFAULT_TTL_MINUTES, e.toString());
            return DEFAULT_TTL_MINUTES;
        }
    }
}
