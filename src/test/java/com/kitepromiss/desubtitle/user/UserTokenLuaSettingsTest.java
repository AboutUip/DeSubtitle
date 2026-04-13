package com.kitepromiss.desubtitle.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class UserTokenLuaSettingsTest {

    @Test
    void readsTtlFromLua(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("user_token.lua");
        Files.writeString(lua, "return { token_ttl_minutes = 120 }\n");
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                lua,
                temp.resolve("video_upload.lua"));
        assertEquals(120, new UserTokenLuaSettings(paths).tokenTtlMinutes());
    }

    @Test
    void clampsHighValue(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("user_token.lua");
        Files.writeString(lua, "return { token_ttl_minutes = 999999 }\n");
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                lua,
                temp.resolve("video_upload.lua"));
        assertEquals(UserTokenLuaSettings.MAX_TTL_MINUTES, new UserTokenLuaSettings(paths).tokenTtlMinutes());
    }

    @Test
    void missingFileUsesDefault(@TempDir Path temp) {
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("nope.lua"),
                temp.resolve("video_upload.lua"));
        assertEquals(UserTokenLuaSettings.DEFAULT_TTL_MINUTES, new UserTokenLuaSettings(paths).tokenTtlMinutes());
    }
}
