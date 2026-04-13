package com.kitepromiss.desubtitle.workspace;

import java.nio.file.Path;

/**
 * 解析于 JVM 工作目录的仓库根相对路径：运行态 JSON、协议 JSON、Lua 参考与 {@code data/} 数据区。
 */
public record WorkspacePaths(
        Path runtimeJson,
        Path runtimeModeLua,
        Path dataDirectory,
        Path agreementJson,
        Path userTokenLua,
        Path videoUploadLua) {

    public static WorkspacePaths fromWorkingDirectory() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return new WorkspacePaths(
                cwd.resolve("config/json/runtime.json").normalize(),
                cwd.resolve("config/lua/runtime_mode.lua").normalize(),
                cwd.resolve("data").normalize(),
                cwd.resolve("config/json/agreement.json").normalize(),
                cwd.resolve("config/lua/user_token.lua").normalize(),
                cwd.resolve("config/lua/video_upload.lua").normalize());
    }
}
