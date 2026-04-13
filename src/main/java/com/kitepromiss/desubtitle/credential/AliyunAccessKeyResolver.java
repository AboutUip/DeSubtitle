package com.kitepromiss.desubtitle.credential;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.credential.InMemoryAliyunCredentialHolder.Pair;
import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;

/**
 * 解析当前可用于视觉智能 API 的 AccessKey：非调试优先读库，否则读调试内存；二者皆空时回退环境变量。
 */
@Service
public class AliyunAccessKeyResolver implements AliyunCredentialsSource {

    private final AliyunCredentialSetupService credentialSetupService;
    private final InMemoryAliyunCredentialHolder inMemoryAliyunCredentialHolder;
    private final AliyunCredentialsRepository credentialsRepository;
    private final SqliteConcurrencyController sqliteConcurrencyController;

    public AliyunAccessKeyResolver(
            AliyunCredentialSetupService credentialSetupService,
            InMemoryAliyunCredentialHolder inMemoryAliyunCredentialHolder,
            AliyunCredentialsRepository credentialsRepository,
            SqliteConcurrencyController sqliteConcurrencyController) {
        this.credentialSetupService = credentialSetupService;
        this.inMemoryAliyunCredentialHolder = inMemoryAliyunCredentialHolder;
        this.credentialsRepository = credentialsRepository;
        this.sqliteConcurrencyController = sqliteConcurrencyController;
    }

    @Override
    public Optional<ResolvedAliyunKeys> resolve() {
        if (credentialSetupService.isDebugMode()) {
            Optional<Pair> m = inMemoryAliyunCredentialHolder.get();
            if (m.isPresent()) {
                Pair p = m.get();
                if (!p.accessKeyId().isBlank() && !p.accessKeySecret().isBlank()) {
                    return Optional.of(new ResolvedAliyunKeys(p.accessKeyId(), p.accessKeySecret()));
                }
            }
        } else {
            Optional<ResolvedAliyunKeys> fromDb =
                    sqliteConcurrencyController.supply(this::loadFromDbIfComplete);
            if (fromDb.isPresent()) {
                return fromDb;
            }
        }
        return fromEnvironment();
    }

    private Optional<ResolvedAliyunKeys> loadFromDbIfComplete() {
        return credentialsRepository
                .findById(AliyunCredentialsEntity.SINGLETON_ID)
                .filter(e -> e.getAccessKeyId() != null
                        && !e.getAccessKeyId().isBlank()
                        && e.getAccessKeySecret() != null
                        && !e.getAccessKeySecret().isBlank())
                .map(e -> new ResolvedAliyunKeys(e.getAccessKeyId(), e.getAccessKeySecret()));
    }

    private static Optional<ResolvedAliyunKeys> fromEnvironment() {
        String id = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String secret = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        if (id == null
                || secret == null
                || id.isBlank()
                || secret.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedAliyunKeys(id, secret));
    }
}
