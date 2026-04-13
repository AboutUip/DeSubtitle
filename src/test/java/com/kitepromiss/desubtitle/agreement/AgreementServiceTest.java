package com.kitepromiss.desubtitle.agreement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class AgreementServiceTest {

    @Test
    void returnsTextField(@TempDir Path temp) throws Exception {
        Path agreement = temp.resolve("agreement.json");
        Files.writeString(agreement, "{\"text\":\"条款全文\"}\n");
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                agreement,
                temp.resolve("ut.lua"),
                temp.resolve("video_upload.lua"));
        assertEquals("条款全文", new AgreementService(paths).readAgreementText());
    }

    @Test
    void missingFileReturnsEmpty(@TempDir Path temp) {
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("missing.json"),
                temp.resolve("ut.lua"),
                temp.resolve("video_upload.lua"));
        assertEquals("", new AgreementService(paths).readAgreementText());
    }
}
