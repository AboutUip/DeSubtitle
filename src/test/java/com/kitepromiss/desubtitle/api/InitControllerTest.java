package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.kitepromiss.desubtitle.credential.CredentialInitPrecondition;
import com.kitepromiss.desubtitle.credential.MissingAliyunCredentialsException;
import com.kitepromiss.desubtitle.init.ConcurrentInitInProgressException;
import com.kitepromiss.desubtitle.init.InitializationAccessGate;
import com.kitepromiss.desubtitle.init.InitExecutionMutex;
import com.kitepromiss.desubtitle.init.InitService;
import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class InitControllerTest {

    private static final WorkspacePaths DUMMY_PATHS = new WorkspacePaths(
            Path.of("a.json"),
            Path.of("b.lua"),
            Path.of("data"),
            Path.of("agreement.json"),
            Path.of("user_token.lua"),
            Path.of("video_upload.lua"));

    private static final InitializationAccessGate DUMMY_GATE = new InitializationAccessGate(DUMMY_PATHS);

    private static final InitExecutionMutex DUMMY_MUTEX = new InitExecutionMutex();

    private static final CredentialInitPrecondition DUMMY_PRE = () -> false;

    private static final SqliteConcurrencyController DUMMY_SQLITE = new SqliteConcurrencyController();

    @Test
    void skippedResponse() throws Exception {
        InitService svc = new InitService(DUMMY_PATHS, dummyDataSource(), DUMMY_GATE, DUMMY_MUTEX, DUMMY_PRE, DUMMY_SQLITE) {
            @Override
            public InitRunOutcome run() {
                return new InitRunOutcome(true, false, false);
            }
        };
        InitController c = new InitController(svc);
        ResponseEntity<?> res = c.init();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        InitController.InitResponse r = assertBody(res, InitController.InitResponse.class);
        assertEquals("skipped_already_initialized", r.status());
        assertFalse(r.executed());
        assertFalse(r.initializationFlagWritten());
    }

    @Test
    void completedResponse() throws Exception {
        InitService svc = new InitService(DUMMY_PATHS, dummyDataSource(), DUMMY_GATE, DUMMY_MUTEX, DUMMY_PRE, DUMMY_SQLITE) {
            @Override
            public InitRunOutcome run() {
                return new InitRunOutcome(false, true, false);
            }
        };
        InitController c = new InitController(svc);
        ResponseEntity<?> res = c.init();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        InitController.InitResponse r = assertBody(res, InitController.InitResponse.class);
        assertEquals("completed", r.status());
        assertTrue(r.executed());
        assertTrue(r.debugMode());
        assertFalse(r.initializationFlagWritten());
    }

    @Test
    void concurrentInitReturns409() {
        InitService svc = new InitService(DUMMY_PATHS, dummyDataSource(), DUMMY_GATE, DUMMY_MUTEX, DUMMY_PRE, DUMMY_SQLITE) {
            @Override
            public InitRunOutcome run() {
                throw new ConcurrentInitInProgressException();
            }
        };
        InitController c = new InitController(svc);
        ResponseEntity<?> res = c.init();
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        InitController.InitConflictBody b = assertBody(res, InitController.InitConflictBody.class);
        assertEquals("init_in_progress", b.error());
    }

    @Test
    void needCredentialsReturns428() {
        InitService svc = new InitService(DUMMY_PATHS, dummyDataSource(), DUMMY_GATE, DUMMY_MUTEX, DUMMY_PRE, DUMMY_SQLITE) {
            @Override
            public InitRunOutcome run() {
                throw new MissingAliyunCredentialsException();
            }
        };
        InitController c = new InitController(svc);
        ResponseEntity<?> res = c.init();
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, res.getStatusCode());
        InitController.InitConflictBody b = assertBody(res, InitController.InitConflictBody.class);
        assertEquals("need_credentials", b.error());
    }

    @Test
    void ioExceptionBecomes500() {
        InitService svc = new InitService(DUMMY_PATHS, dummyDataSource(), DUMMY_GATE, DUMMY_MUTEX, DUMMY_PRE, DUMMY_SQLITE) {
            @Override
            public InitRunOutcome run() throws IOException {
                throw new IOException("disk");
            }
        };
        InitController c = new InitController(svc);
        ResponseEntity<?> res = c.init();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        InitController.InitConflictBody b = assertBody(res, InitController.InitConflictBody.class);
        assertEquals("init_failed", b.error());
    }

    private static <T> T assertBody(ResponseEntity<?> res, Class<T> type) {
        assertInstanceOf(type, res.getBody());
        return type.cast(res.getBody());
    }

    private static DriverManagerDataSource dummyDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite::memory:");
        return ds;
    }
}
