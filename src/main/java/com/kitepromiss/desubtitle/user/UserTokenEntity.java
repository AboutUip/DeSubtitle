package com.kitepromiss.desubtitle.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 匿名用户 JWT 的持久化行：与 JWT {@code jti} 对齐，便于撤销与运维查询（动态管理）。
 */
@Entity
@Table(name = "user_tokens")
public class UserTokenEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String jti;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserTokenEntity() {}

    public UserTokenEntity(String jti, String userId, Instant expiresAt, boolean revoked, Instant createdAt) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public String getJti() {
        return jti;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
