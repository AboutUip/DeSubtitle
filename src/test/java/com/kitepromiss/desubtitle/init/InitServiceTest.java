package com.kitepromiss.desubtitle.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.credential.CredentialInitPrecondition;
import com.kitepromiss.desubtitle.credential.MissingAliyunCredentialsException;
import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;

class InitServiceTest {

    @Test
    void skipsWhenAlreadyInitialized(@TempDir Path temp) throws Exception {
        Path json = temp.resolve("runtime.json");
        Files.writeString(json, "{\"initialization_completed\": true}\n");
        writeRuntimeModeLua(temp, "return { debug_mode = false }\n");

        InitService svc = createService(temp, json);
        InitService.InitRunOutcome o = svc.run();

        assertTrue(o.skipped());
        assertFalse(o.initializationFlagWritten());
    }

    @Test
    void createsDataDirAndWritesFlagWhenNotDebug(@TempDir Path temp) throws Exception {
        Path json = temp.resolve("runtime.json");
        Files.writeString(json, "{\"initialization_completed\": false}\n");
        writeRuntimeModeLua(temp, "return { debug_mode = false }\n");

        InitService svc = createService(temp, json);
        InitService.InitRunOutcome o = svc.run();

        assertFalse(o.skipped());
        assertTrue(o.initializationFlagWritten());
        assertTrue(Files.isDirectory(temp.resolve("data")));
        assertTrue(Files.isRegularFile(temp.resolve("data").resolve("desubtitle.db")));

        JsonNode root = JsonConfigLoader.loadTree(json);
        assertTrue(root.path("initialization_completed").booleanValue());
    }

    @Test
    void debugModeDoesNotWriteFlag(@TempDir Path temp) throws Exception {
        Path json = temp.resolve("runtime.json");
        Files.writeString(json, "{\"initialization_completed\": false}\n");
        writeRuntimeModeLua(temp, "return { debug_mode = true }\n");

        InitService svc = createService(temp, json, () -> true);
        InitService.InitRunOutcome o = svc.run();

        assertFalse(o.skipped());
        assertTrue(o.debugMode());
        assertFalse(o.initializationFlagWritten());

        JsonNode root = JsonConfigLoader.loadTree(json);
        assertFalse(root.path("initialization_completed").booleanValue());
    }

    @Test
    void existingDataDirIsPurgedThenReinitialized(@TempDir Path temp) throws Exception {
        Path json = temp.resolve("runtime.json");
        Files.writeString(json, "{\"initialization_completed\": false}\n");
        writeRuntimeModeLua(temp, "return { debug_mode = false }\n");
        Path data = temp.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve("junk.txt"), "x");

        InitService svc = createService(temp, json);
        svc.run();

        assertFalse(Files.exists(data.resolve("junk.txt")));
        assertTrue(Files.isRegularFile(data.resolve("desubtitle.db")));
    }

    @Test
    void failsWhenCredentialsMissing(@TempDir Path temp) throws Exception {
        Path json = temp.resolve("runtime.json");
        Files.writeString(json, "{\"initialization_completed\": false}\n");
        writeRuntimeModeLua(temp, "return { debug_mode = false }\n");

        InitService svc = createService(temp, json, () -> {
            throw new MissingAliyunCredentialsException();
        });
        assertThrows(MissingAliyunCredentialsException.class, svc::run);
    }

    private static void writeRuntimeModeLua(Path temp, String luaBody) throws Exception {
        Files.writeString(temp.resolve("runtime_mode.lua"), luaBody);
    }

    private static InitService createService(Path temp, Path runtimeJson) {
        return createService(temp, runtimeJson, () -> false);
    }

    private static InitService createService(Path temp, Path runtimeJson, CredentialInitPrecondition precondition) {
        WorkspacePaths paths = new WorkspacePaths(
                runtimeJson,
                temp.resolve("runtime_mode.lua"),
                temp.resolve("data"),
                temp.resolve("agreement.json"),
                temp.resolve("user_token.lua"),
                temp.resolve("video_upload.lua"));
        Path db = paths.dataDirectory().resolve("desubtitle.db");
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl(toSqliteJdbcUrl(db));
        InitializationAccessGate gate = new InitializationAccessGate(paths);
        return new InitService(paths, ds, gate, new InitExecutionMutex(), precondition, new SqliteConcurrencyController());
    }

    private static String toSqliteJdbcUrl(Path dbFile) {
        return "jdbc:sqlite:" + dbFile.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
