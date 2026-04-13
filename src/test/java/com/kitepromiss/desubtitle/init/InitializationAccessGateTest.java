package com.kitepromiss.desubtitle.init;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class InitializationAccessGateTest {

    @Test
    void detectsDeletionOfRuntimeJsonAfterCacheWasTrue(@TempDir Path temp) throws Exception {
        Path runtime = temp.resolve("runtime.json");
        Path data = temp.resolve("data");
        Files.createDirectories(data);
        Files.writeString(runtime, "{\"initialization_completed\":true}\n");
        WorkspacePaths paths =
                new WorkspacePaths(runtime, temp.resolve("m.lua"), data, temp.resolve("a.json"), temp.resolve("u.lua"), temp.resolve("v.lua"));
        InitializationAccessGate gate = new InitializationAccessGate(paths);
        assertTrue(gate.isInitializationComplete());
        Files.deleteIfExists(runtime);
        assertFalse(gate.isInitializationComplete());
    }
}
