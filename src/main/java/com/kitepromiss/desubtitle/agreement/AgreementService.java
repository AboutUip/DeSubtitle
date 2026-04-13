package com.kitepromiss.desubtitle.agreement;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;

/**
 * 从 {@code config/json/agreement.json} 读取用户协议正文（字段 {@code text}）。
 */
@Service
public class AgreementService {

    /** 与 {@code agreement.json} 中键名一致；写入协议全文时请使用该字段。 */
    public static final String JSON_KEY_TEXT = "text";

    private final WorkspacePaths workspacePaths;

    public AgreementService(WorkspacePaths workspacePaths) {
        this.workspacePaths = workspacePaths;
    }

    /**
     * @return 协议字符串；文件不存在、非 JSON 对象或缺少 {@value #JSON_KEY_TEXT} 时返回空字符串
     */
    public String readAgreementText() {
        var path = workspacePaths.agreementJson();
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            JsonNode root = JsonConfigLoader.loadTree(path);
            if (!root.isObject()) {
                return "";
            }
            return root.path(JSON_KEY_TEXT).asText("");
        } catch (IOException e) {
            return "";
        }
    }
}
