package com.kitepromiss.desubtitle.credential;

import org.springframework.stereotype.Component;

@Component
public class AliyunCredentialInitBridge implements CredentialInitPrecondition {

    private final AliyunCredentialSetupService credentialSetupService;

    public AliyunCredentialInitBridge(AliyunCredentialSetupService credentialSetupService) {
        this.credentialSetupService = credentialSetupService;
    }

    @Override
    public boolean assertKeysPresentAndGetDebugMode() {
        boolean debugMode = credentialSetupService.isDebugMode();
        if (!credentialSetupService.hasCredentialsForInit(debugMode)) {
            throw new MissingAliyunCredentialsException();
        }
        return debugMode;
    }
}
