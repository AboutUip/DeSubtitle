package com.kitepromiss.desubtitle.user;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;

/**
 * 对 {@code user_tokens} 的撤销等运维操作；须经 {@link SqliteConcurrencyController} 与 SQLite 交互。
 */
@Service
public class UserTokenManagementService {

    private final SqliteConcurrencyController sqliteConcurrencyController;
    private final UserTokenRepository userTokenRepository;

    public UserTokenManagementService(
            SqliteConcurrencyController sqliteConcurrencyController, UserTokenRepository userTokenRepository) {
        this.sqliteConcurrencyController = sqliteConcurrencyController;
        this.userTokenRepository = userTokenRepository;
    }

    /** 按 {@code jti} 标记撤销；无对应行时不报错。 */
    public void revokeByJti(String jti) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        sqliteConcurrencyController.run(() -> userTokenRepository
                .findById(jti)
                .ifPresent(e -> {
                    e.setRevoked(true);
                    userTokenRepository.save(e);
                }));
    }
}
