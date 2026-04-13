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
 * 在已有 SQLite 库缺少 {@code expires_at} 时补齐列并回填，避免 Hibernate {@code update} 在 SQLite 上未改表导致运行时失败。
 */
@Component
public class UserVideoExpiresAtSqliteMigration {

    private static final Logger log = LoggerFactory.getLogger(UserVideoExpiresAtSqliteMigration.class);

    private final DataSource dataSource;

    public UserVideoExpiresAtSqliteMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void addExpiresAtColumnIfMissing() {
        try (Connection c = dataSource.getConnection()) {
            if (!tableExists(c, "user_videos") || columnExists(c, "user_videos", "expires_at")) {
                return;
            }
            try (Statement s = c.createStatement()) {
                s.executeUpdate("ALTER TABLE user_videos ADD COLUMN expires_at BIGINT");
            }
            try (Statement s = c.createStatement()) {
                try {
                    s.executeUpdate(
                            "UPDATE user_videos SET expires_at = (CAST(strftime('%s', created_at) AS INTEGER) + 300) * 1000 "
                                    + "WHERE expires_at IS NULL");
                } catch (SQLException ex) {
                    log.debug("按 created_at 回填 expires_at 跳过: {}", ex.getMessage());
                }
            }
            try (Statement s = c.createStatement()) {
                s.executeUpdate(
                        "UPDATE user_videos SET expires_at = (CAST(strftime('%s', 'now') AS INTEGER) + 300) * 1000 "
                                + "WHERE expires_at IS NULL");
            }
            log.info("已为 user_videos 增加 expires_at 并完成回填");
        } catch (SQLException e) {
            log.warn("user_videos.expires_at 迁移未执行或部分失败（可删除 data/desubtitle.db 后重试）: {}", e.toString());
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
