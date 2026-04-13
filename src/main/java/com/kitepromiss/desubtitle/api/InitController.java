package com.kitepromiss.desubtitle.api;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.credential.MissingAliyunCredentialsException;
import com.kitepromiss.desubtitle.init.ConcurrentInitInProgressException;
import com.kitepromiss.desubtitle.init.InitService;

/**
 * 应用数据区与运行态配置的首次初始化入口。
 */
@RestController
public class InitController {

    private static final Logger log = LoggerFactory.getLogger(InitController.class);

    private final InitService initService;

    public InitController(InitService initService) {
        this.initService = initService;
    }

    /**
     * 无查询参数、无请求体；语义见 {@code docs/api/init.md}。
     */
    @PostMapping("/init")
    public ResponseEntity<?> init() {
        try {
            InitService.InitRunOutcome o = initService.run();
            if (o.skipped()) {
                return ResponseEntity.ok(new InitResponse("skipped_already_initialized", false, false, false));
            }
            return ResponseEntity.ok(new InitResponse("completed", true, o.debugMode(), o.initializationFlagWritten()));
        } catch (ConcurrentInitInProgressException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new InitConflictBody("init_in_progress"));
        } catch (MissingAliyunCredentialsException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(new InitConflictBody("need_credentials"));
        } catch (IOException e) {
            log.warn("初始化 IO 失败: {}", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InitConflictBody("init_failed"));
        }
    }

    /**
     * @param executed           是否实际执行了初始化（未因已初始化而跳过）
     * @param debugMode          本次执行时是否处于 debug_mode
     * @param initializationFlagWritten 是否在 runtime.json 写入了 {@code initialization_completed=true}
     */
    public record InitResponse(
            String status,
            boolean executed,
            boolean debugMode,
            boolean initializationFlagWritten) {}

    /** HTTP 409 时与成功体区分，仅含错误码。 */
    public record InitConflictBody(String error) {}
}
