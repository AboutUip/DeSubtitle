package com.kitepromiss.desubtitle.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.credential.CredentialInitPrecondition;
import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import com.zaxxer.hikari.HikariDataSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 首次初始化：维护 {@code data/} 与 SQLite 文件，并在非调试模式下将 {@code runtime.json} 标记为已完成初始化。
 */
@Service
public class InitService {

    private static final Logger log = LoggerFactory.getLogger(InitService.class);

    /** 与 {@code spring.datasource.url} 中 SQLite 文件名一致；清空 {@code data/} 时保留此文件以免删掉已写入的 AccessKey。 */
    public static final String DESUBTITLE_DB_FILE_NAME = "desubtitle.db";

    private final WorkspacePaths paths;
    private final DataSource dataSource;
    private final InitializationAccessGate initializationAccessGate;
    private final InitExecutionMutex initExecutionMutex;
    private final CredentialInitPrecondition credentialInitPrecondition;
    private final SqliteConcurrencyController sqliteConcurrencyController;

    public InitService(
            WorkspacePaths paths,
            DataSource dataSource,
            InitializationAccessGate initializationAccessGate,
            InitExecutionMutex initExecutionMutex,
            CredentialInitPrecondition credentialInitPrecondition,
            SqliteConcurrencyController sqliteConcurrencyController) {
        this.paths = paths;
        this.dataSource = dataSource;
        this.initializationAccessGate = initializationAccessGate;
        this.initExecutionMutex = initExecutionMutex;
        this.credentialInitPrecondition = credentialInitPrecondition;
        this.sqliteConcurrencyController = sqliteConcurrencyController;
    }

    /**
     * @param skipped 因 {@code initialization_completed} 已为 true 而跳过
     * @param debugMode 本次执行读取到的 debug_mode（跳过时为 false）
     * @param initializationFlagWritten 是否在非 debug 下写入了 {@code initialization_completed=true}
     */
    public record InitRunOutcome(boolean skipped, boolean debugMode, boolean initializationFlagWritten) {}

    public InitRunOutcome run() throws IOException {
        if (isAlreadyInitialized()) {
            initializationAccessGate.syncStateAfterInitRun(false);
            return new InitRunOutcome(true, false, false);
        }

        if (!initExecutionMutex.tryAcquireExclusive()) {
            throw new ConcurrentInitInProgressException();
        }
        try {
            if (isAlreadyInitialized()) {
                initializationAccessGate.syncStateAfterInitRun(false);
                return new InitRunOutcome(true, false, false);
            }

            boolean debugMode = credentialInitPrecondition.assertKeysPresentAndGetDebugMode();

            initializationAccessGate.beginInitExecution();
            AtomicBoolean wroteCompletionFlag = new AtomicBoolean(false);
            try {
                Path dataDir = paths.dataDirectory();

                if (!Files.isDirectory(dataDir)) {
                    Files.createDirectories(dataDir);
                    standardInitialization();
                } else {
                    evictHikariConnections();
                    purgeDataDirectoryContents(dataDir);
                    evictHikariConnections();
                    standardInitialization();
                }

                if (!debugMode) {
                    writeInitializationCompletedTrue();
                    wroteCompletionFlag.set(true);
                } else {
                    log.debug("debug_mode 为 true，跳过将 initialization_completed 写回 runtime.json");
                }

                return new InitRunOutcome(false, debugMode, wroteCompletionFlag.get());
            } finally {
                initializationAccessGate.endInitExecution();
                initializationAccessGate.syncStateAfterInitRun(wroteCompletionFlag.get());
            }
        } finally {
            initExecutionMutex.releaseExclusive();
        }
    }

    private boolean isAlreadyInitialized() throws IOException {
        Path p = paths.runtimeJson();
        if (!Files.isRegularFile(p)) {
            return false;
        }
        JsonNode n = JsonConfigLoader.loadTree(p);
        return n.path("initialization_completed").asBoolean(false);
    }

    private void standardInitialization() throws IOException {
        touchSqliteViaDataSource();
        runPostInitPlaceholder();
    }

    /**
     * 通过当前数据源建立连接，确保 SQLite 文件存在（由 JDBC 创建空库）。
     */
    private void touchSqliteViaDataSource() {
        sqliteConcurrencyController.run(() -> {
            try (Connection c = dataSource.getConnection();
                    Statement st = c.createStatement()) {
                st.execute("SELECT 1");
            } catch (SQLException e) {
                throw new IllegalStateException("初始化 SQLite 失败", e);
            }
        });
    }

    /**
     * 后续业务初始化（迁移、种子数据等）占位。
     */
    private void runPostInitPlaceholder() {
        // 预留
    }

    private void writeInitializationCompletedTrue() throws IOException {
        Path p = paths.runtimeJson();
        JsonNode root;
        if (Files.isRegularFile(p)) {
            root = JsonConfigLoader.loadTree(p);
        } else {
            root = JsonNodeFactory.instance.objectNode();
        }
        if (!root.isObject()) {
            throw new IllegalStateException("runtime.json 根须为 JSON 对象");
        }
        ObjectNode obj = (ObjectNode) root;
        obj.put("initialization_completed", true);
        JsonConfigLoader.writeTree(p, obj);
    }

    private void evictHikariConnections() {
        if (dataSource instanceof HikariDataSource h) {
            h.getHikariPoolMXBean().softEvictConnections();
        }
    }

    /**
     * 删除 {@code data/} 下全部内容，保留目录本身。
     */
    static void purgeDataDirectoryContents(Path dataDir) throws IOException {
        if (!Files.isDirectory(dataDir)) {
            return;
        }
        try (Stream<Path> list = Files.list(dataDir)) {
            for (Path child : list.toList()) {
                if (DESUBTITLE_DB_FILE_NAME.equals(child.getFileName().toString())) {
                    continue;
                }
                deleteRecursive(child);
            }
        }
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (Files.isDirectory(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        } else {
            Files.deleteIfExists(root);
        }
    }
}
