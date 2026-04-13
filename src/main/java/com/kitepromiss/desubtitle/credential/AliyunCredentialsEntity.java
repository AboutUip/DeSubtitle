package com.kitepromiss.desubtitle.credential;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 单行表：用户通过引导页提交的阿里云 AccessKey（明文）；安全责任见 {@code docs/reference/aliyun-credentials-storage.md}。
 */
@Entity
@Table(name = "aliyun_credentials")
public class AliyunCredentialsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false, length = 256)
    private String accessKeyId;

    @Column(nullable = false, length = 512)
    private String accessKeySecret;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
}
