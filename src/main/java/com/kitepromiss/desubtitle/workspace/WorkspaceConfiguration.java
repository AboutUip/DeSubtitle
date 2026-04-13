package com.kitepromiss.desubtitle.workspace;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkspaceConfiguration {

    @Bean
    public WorkspacePaths workspacePaths() {
        return WorkspacePaths.fromWorkingDirectory();
    }
}
