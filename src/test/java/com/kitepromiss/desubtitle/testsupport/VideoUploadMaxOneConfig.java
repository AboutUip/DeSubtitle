package com.kitepromiss.desubtitle.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.kitepromiss.desubtitle.video.VideoUploadLuaSettings;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

@TestConfiguration
@Profile("video-upload-quota-one")
public class VideoUploadMaxOneConfig {

    @Bean
    @Primary
    public VideoUploadLuaSettings videoUploadLuaSettingsQuotaOne(WorkspacePaths paths) {
        return new VideoUploadLuaSettings(paths) {
            @Override
            public int maxVideosPerUser() {
                return 1;
            }
        };
    }
}
