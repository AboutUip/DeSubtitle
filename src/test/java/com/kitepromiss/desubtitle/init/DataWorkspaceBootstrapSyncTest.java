package com.kitepromiss.desubtitle.init;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;

class DataWorkspaceBootstrapSyncTest {

    @Test
    void resetsRuntimeWhenDataDirMissingButFlagWasTrue(@TempDir Path temp) throws Exception {
        Path data = temp.resolve("data");
        Path runtime = temp.resolve("runtime.json");
        Files.writeString(runtime, "{\"initialization_completed\": true}\n");
        WorkspacePaths paths =
                new WorkspacePaths(runtime, temp.resolve("m.lua"), data, temp.resolve("a.json"), temp.resolve("u.lua"), temp.resolve("v.lua"));

        assertTrue(DataWorkspaceBootstrapSync.resetInitializationFlagIfDataWorkspaceGone(paths));
        JsonNode n = JsonConfigLoader.loadTree(runtime);
        assertFalse(n.path("initialization_completed").booleanValue());
    }

    @Test
    void noOpWhenDataDirAndDbExist(@TempDir Path temp) throws Exception {
        Path data = temp.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve(InitService.DESUBTITLE_DB_FILE_NAME), "");
        Path runtime = temp.resolve("runtime.json");
        Files.writeString(runtime, "{\"initialization_completed\": true}\n");
        WorkspacePaths paths =
                new WorkspacePaths(runtime, temp.resolve("m.lua"), data, temp.resolve("a.json"), temp.resolve("u.lua"), temp.resolve("v.lua"));

        assertFalse(DataWorkspaceBootstrapSync.resetInitializationFlagIfDataWorkspaceGone(paths));
        JsonNode n = JsonConfigLoader.loadTree(runtime);
        assertTrue(n.path("initialization_completed").booleanValue());
    }

    @Test
    void resetsWhenDataDirExistsButDbMissing(@TempDir Path temp) throws Exception {
        Path data = temp.resolve("data");
        Files.createDirectories(data);
        Path runtime = temp.resolve("runtime.json");
        Files.writeString(runtime, "{\"initialization_completed\": true}\n");
        WorkspacePaths paths =
                new WorkspacePaths(runtime, temp.resolve("m.lua"), data, temp.resolve("a.json"), temp.resolve("u.lua"), temp.resolve("v.lua"));

        assertTrue(DataWorkspaceBootstrapSync.resetInitializationFlagIfDataWorkspaceGone(paths));
        JsonNode n = JsonConfigLoader.loadTree(runtime);
        assertFalse(n.path("initialization_completed").booleanValue());
    }
}
