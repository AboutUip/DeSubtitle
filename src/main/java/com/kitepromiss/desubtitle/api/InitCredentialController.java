package com.kitepromiss.desubtitle.api;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.credential.AliyunCredentialSetupService;

/**
 * 用户提交阿里云 AccessKey；非调试模式写入 SQLite，调试模式仅进程内内存。
 */
@RestController
public class InitCredentialController {

    private static final int ACCESS_KEY_ID_MAX = 256;
    private static final int ACCESS_KEY_SECRET_MAX = 512;

    private final AliyunCredentialSetupService credentialSetupService;

    public InitCredentialController(AliyunCredentialSetupService credentialSetupService) {
        this.credentialSetupService = credentialSetupService;
    }

    @PostMapping("/init/credentials")
    public ResponseEntity<?> store(@RequestBody CredentialRequest request) {
        String id = request.accessKeyId() == null ? "" : request.accessKeyId().trim();
        String secret = request.accessKeySecret() == null ? "" : request.accessKeySecret().trim();
        if (id.isEmpty() || secret.isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "access_key_empty"));
        }
        if (id.length() > ACCESS_KEY_ID_MAX || secret.length() > ACCESS_KEY_SECRET_MAX) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "access_key_too_long"));
        }
        boolean debugBefore = credentialSetupService.isDebugMode();
        credentialSetupService.storeFromUser(id, secret);
        return ResponseEntity.ok(new CredentialStoreResponse(true, !debugBefore));
    }

    public record CredentialRequest(String accessKeyId, String accessKeySecret) {}

    /**
     * @param persisted {@code true} 表示已写入 SQLite；调试模式下为 {@code false}。
     */
    public record CredentialStoreResponse(boolean stored, boolean persisted) {}
}
