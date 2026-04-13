package com.kitepromiss.desubtitle.video;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 在已有 SQLite 库缺少去字幕相关列时补齐，避免 Hibernate {@code update} 未改表导致运行时失败。
 */
@Component
public class UserVideoDesubtitleSqliteMigration {

    private static final Logger log = LoggerFactory.getLogger(UserVideoDesubtitleSqliteMigration.class);

    private final DataSource dataSource;

    public UserVideoDesubtitleSqliteMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void addDesubtitleColumnsIfMissing() {
        try (Connection c = dataSource.getConnection()) {
            if (!tableExists(c, "user_videos")) {
                return;
            }
            addColumnIfMissing(c, "user_videos", "desubtitle_job_id", "VARCHAR(128)");
            addColumnIfMissing(c, "user_videos", "desubtitle_last_status", "VARCHAR(64)");
            addColumnIfMissing(c, "user_videos", "desubtitle_error", "VARCHAR(2048)");
            addColumnIfMissing(c, "user_videos", "desubtitle_output_file_name", "VARCHAR(256)");
            addColumnIfMissing(c, "user_videos", "desubtitle_output_expires_at", "BIGINT");
        } catch (SQLException e) {
            log.warn("user_videos 去字幕列迁移未执行或失败（可删除 data/desubtitle.db 后重试）: {}", e.toString());
        }
    }

    private static void addColumnIfMissing(Connection c, String table, String column, String sqlType) throws SQLException {
        if (columnExists(c, table, column)) {
            return;
        }
        try (Statement s = c.createStatement()) {
            s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + sqlType);
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getTables(null, null, table, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
