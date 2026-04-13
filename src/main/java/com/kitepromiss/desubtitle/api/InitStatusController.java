package com.kitepromiss.desubtitle.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.credential.AliyunCredentialSetupService;
import com.kitepromiss.desubtitle.init.InitializationAccessGate;

/**
 * 引导前端在未完成初始化时展示 AccessKey 表单等。
 */
@RestController
public class InitStatusController {

    private final InitializationAccessGate initializationAccessGate;
    private final AliyunCredentialSetupService credentialSetupService;

    public InitStatusController(
            InitializationAccessGate initializationAccessGate,
            AliyunCredentialSetupService credentialSetupService) {
        this.initializationAccessGate = initializationAccessGate;
        this.credentialSetupService = credentialSetupService;
    }

    @GetMapping("/init/status")
    public InitStatusResponse status() {
        boolean initialized = initializationAccessGate.isInitializationComplete();
        boolean debugMode = credentialSetupService.isDebugMode();
        boolean credentialsConfigured = credentialSetupService.credentialsConfigured(debugMode);
        return new InitStatusResponse(initialized, debugMode, credentialsConfigured);
    }

    public record InitStatusResponse(boolean initialized, boolean debugMode, boolean credentialsConfigured) {}
}
