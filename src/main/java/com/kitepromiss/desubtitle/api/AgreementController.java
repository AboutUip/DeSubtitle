package com.kitepromiss.desubtitle.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.agreement.AgreementService;

/**
 * 向前端返回 {@code config/json/agreement.json} 中的协议正文（纯文本）。
 */
@RestController
public class AgreementController {

    private final AgreementService agreementService;

    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    /**
     * 无查询参数、无路径变量；正文为 UTF-8 纯文本。
     */
    @GetMapping(value = "/getAgreement", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String getAgreement() {
        return agreementService.readAgreementText();
    }
}
