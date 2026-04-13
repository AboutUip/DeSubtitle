package com.kitepromiss.desubtitle.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteJdbcFileSupportTest {

    @Test
    void createsParentDirectoriesForNestedFilePath(@TempDir Path tmp) throws Exception {
        Path db = tmp.resolve("nested").resolve("desubtitle.db");
        assertFalse(Files.exists(db.getParent()));
        SqliteJdbcFileSupport.ensureParentDirectoryExists(
                "jdbc:sqlite:" + db.toAbsolutePath().normalize().toString().replace('\\', '/'));
        assertTrue(Files.isDirectory(db.getParent()));
    }

    @Test
    void skipsMemoryDatabase() throws Exception {
        SqliteJdbcFileSupport.ensureParentDirectoryExists("jdbc:sqlite::memory:");
    }

    @Test
    void skipsNonSqliteUrl(@TempDir Path tmp) throws Exception {
        Path nested = tmp.resolve("must-not-be-created");
        Path db = nested.resolve("x.db");
        assertFalse(Files.exists(nested));
        SqliteJdbcFileSupport.ensureParentDirectoryExists("jdbc:h2:" + db.toAbsolutePath());
        assertFalse(Files.exists(nested));
    }
}
