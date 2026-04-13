package com.kitepromiss.desubtitle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kitepromiss.desubtitle.init.DataWorkspaceBootstrapSync;
import com.kitepromiss.desubtitle.config.StartupLuaPorts;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

@SpringBootApplication
@EnableScheduling
public class DeSubtitleApplication {

    public static void main(String[] args) {
        Map<String, Object> fromLua = StartupLuaPorts.loadDefaultPropertiesFromLua();
        try {
            DataWorkspaceBootstrapSync.resetInitializationFlagIfDataWorkspaceGone(WorkspacePaths.fromWorkingDirectory());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SpringApplication app = new SpringApplication(DeSubtitleApplication.class);
        app.setDefaultProperties(fromLua);
        app.run(args);
    }

}
