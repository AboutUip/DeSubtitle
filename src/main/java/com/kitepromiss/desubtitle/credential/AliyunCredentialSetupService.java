package com.kitepromiss.desubtitle.credential;

import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kitepromiss.desubtitle.config.LuaConfigLoader;
import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 读取 {@code runtime_mode.lua} 的 {@code debug_mode}；按模式将 AccessKey 写入 SQLite 或仅内存。
 */
@Service
public class AliyunCredentialSetupService {

    private static final Logger log = LoggerFactory.getLogger(AliyunCredentialSetupService.class);

    private final WorkspacePaths workspacePaths;
    private final AliyunCredentialsRepository credentialsRepository;
    private final InMemoryAliyunCredentialHolder inMemoryAliyunCredentialHolder;
    private final SqliteConcurrencyController sqliteConcurrencyController;

    public AliyunCredentialSetupService(
            WorkspacePaths workspacePaths,
            AliyunCredentialsRepository credentialsRepository,
            InMemoryAliyunCredentialHolder inMemoryAliyunCredentialHolder,
            SqliteConcurrencyController sqliteConcurrencyController) {
        this.workspacePaths = workspacePaths;
        this.credentialsRepository = credentialsRepository;
        this.inMemoryAliyunCredentialHolder = inMemoryAliyunCredentialHolder;
        this.sqliteConcurrencyController = sqliteConcurrencyController;
    }

    public boolean isDebugMode() {
        Path p = workspacePaths.runtimeModeLua();
        if (!Files.isRegularFile(p)) {
            return false;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get("debug_mode");
            return v.isboolean() && v.toboolean();
        } catch (Exception e) {
            log.warn("读取 runtime_mode.lua 失败，按非调试处理: {}", e.toString());
            return false;
        }
    }

    /**
     * 调试模式：仅内存；否则 upsert 至 {@link AliyunCredentialsEntity} 表。
     */
    @Transactional
    public void storeFromUser(String accessKeyId, String accessKeySecret) {
        if (isDebugMode()) {
            inMemoryAliyunCredentialHolder.set(accessKeyId, accessKeySecret);
            return;
        }
        sqliteConcurrencyController.run(() -> {
            AliyunCredentialsEntity e = credentialsRepository
                    .findById(AliyunCredentialsEntity.SINGLETON_ID)
                    .orElseGet(AliyunCredentialsEntity::new);
            e.setId(AliyunCredentialsEntity.SINGLETON_ID);
            e.setAccessKeyId(accessKeyId);
            e.setAccessKeySecret(accessKeySecret);
            credentialsRepository.save(e);
        });
    }

    public boolean hasCredentialsForInit(boolean debugMode) {
        if (debugMode) {
            return inMemoryAliyunCredentialHolder
                    .get()
                    .filter(p -> !p.accessKeyId().isBlank() && !p.accessKeySecret().isBlank())
                    .isPresent();
        }
        return sqliteConcurrencyController.supply(
                () -> credentialsRepository
                        .findById(AliyunCredentialsEntity.SINGLETON_ID)
                        .filter(e -> e.getAccessKeyId() != null
                                && !e.getAccessKeyId().isBlank()
                                && e.getAccessKeySecret() != null
                                && !e.getAccessKeySecret().isBlank())
                        .isPresent());
    }

    public boolean credentialsConfigured(boolean debugMode) {
        return hasCredentialsForInit(debugMode);
    }
}
