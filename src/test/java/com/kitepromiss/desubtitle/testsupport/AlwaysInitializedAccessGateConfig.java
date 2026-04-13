package com.kitepromiss.desubtitle.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.kitepromiss.desubtitle.init.InitializationAccessGate;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 集成测试中覆盖 {@link InitializationAccessGate}，使业务 API 不经真实 {@code runtime.json} 即可访问。
 */
public final class AlwaysInitializedAccessGateConfig {

    private AlwaysInitializedAccessGateConfig() {}

    @TestConfiguration
    public static class Beans {

        @Bean
        @Primary
        public InitializationAccessGate initializationAccessGate(WorkspacePaths paths) {
            return new InitializationAccessGate(paths) {
                @Override
                public boolean isInitializationComplete() {
                    return true;
                }

                @Override
                public boolean isInitExecutionInProgress() {
                    return false;
                }
            };
        }
    }
}
