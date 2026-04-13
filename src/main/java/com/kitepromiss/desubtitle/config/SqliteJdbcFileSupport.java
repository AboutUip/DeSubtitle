package com.kitepromiss.desubtitle.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SQLite JDBC 不会在父目录缺失时自动创建目录；在首次建连前需保证库文件父路径存在。
 */
public final class SqliteJdbcFileSupport {

    private SqliteJdbcFileSupport() {}

    /**
     * @param jdbcUrl {@code spring.datasource.url}；非文件型 SQLite（如 {@code :memory:}）则忽略。
     */
    public static void ensureParentDirectoryExists(String jdbcUrl) throws IOException {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        String trimmed = jdbcUrl.trim();
        final String prefix = "jdbc:sqlite:";
        if (trimmed.length() < prefix.length() || !trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return;
        }
        String pathPart = trimmed.substring(prefix.length()).trim();
        if (pathPart.isEmpty()) {
            return;
        }
        String lower = pathPart.toLowerCase();
        if (lower.startsWith(":memory:")) {
            return;
        }
        Path db = Path.of(pathPart).toAbsolutePath().normalize();
        Path parent = db.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
