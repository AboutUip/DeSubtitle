package com.kitepromiss.desubtitle.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 在 Spring 上下文启动<strong>之前</strong>运行：若工作区 {@code data/} 或 SQLite 库文件已不存在（被用户删除、需由后续启动逻辑重新创建），
 * 而 {@code runtime.json} 仍标记 {@code initialization_completed=true}，则将其写回 {@code false}，使前端重新进入初始化向导并补录
 * AccessKey 等。
 *
 * <p>执行时机早于 {@link com.kitepromiss.desubtitle.config.SqliteDataDirectoryConfiguration} 自动创建库文件父目录，因此能可靠发现「整目录被删」的情形。
 */
public final class DataWorkspaceBootstrapSync {

    private static final Logger log = LoggerFactory.getLogger(DataWorkspaceBootstrapSync.class);

    private DataWorkspaceBootstrapSync() {}

    /**
     * @return {@code true} 若已写回 {@code initialization_completed=false}
     */
    public static boolean resetInitializationFlagIfDataWorkspaceGone(WorkspacePaths paths) throws IOException {
        Path dataDir = paths.dataDirectory();
        Path dbFile = dataDir.resolve(InitService.DESUBTITLE_DB_FILE_NAME);
        boolean dataDirMissing = !Files.isDirectory(dataDir);
        boolean dbMissing = Files.isDirectory(dataDir) && !Files.isRegularFile(dbFile);
        if (!dataDirMissing && !dbMissing) {
            return false;
        }

        Path runtime = paths.runtimeJson();
        if (!Files.isRegularFile(runtime)) {
            return false;
        }
        JsonNode root = JsonConfigLoader.loadTree(runtime);
        if (!root.isObject() || !root.path("initialization_completed").asBoolean(false)) {
            return false;
        }

        ObjectNode obj = (ObjectNode) root;
        obj.put("initialization_completed", false);
        JsonConfigLoader.writeTree(runtime, obj);
        log.warn(
                "检测到 data 工作区需重建（data 目录缺失: {}，或 desubtitle.db 缺失: {}），已将 {} 中 initialization_completed 设为 false",
                dataDirMissing,
                !dataDirMissing && dbMissing,
                runtime);
        return true;
    }
}
