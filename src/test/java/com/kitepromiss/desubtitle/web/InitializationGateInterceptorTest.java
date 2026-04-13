package com.kitepromiss.desubtitle.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import com.kitepromiss.desubtitle.agreement.AgreementService;
import com.kitepromiss.desubtitle.api.AgreementController;
import com.kitepromiss.desubtitle.api.LifeController;
import com.kitepromiss.desubtitle.init.InitializationAccessGate;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class InitializationGateInterceptorTest {

    @Test
    void lifeAlwaysAllowedWhenNotInitialized(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, false);
        InitializationGateInterceptor in = interceptor(temp);

        LifeController life = new LifeController(null, null, null, null, null, 3);
        HandlerMethod hm = new HandlerMethod(life, LifeController.class.getMethod("life", String.class));
        assertTrue(in.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), hm));
    }

    @Test
    void getAgreementAllowedWhenNotInitialized(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, false);
        Files.writeString(temp.resolve("agreement.json"), "{\"text\":\"\"}\n");
        InitializationGateInterceptor in = interceptor(temp);
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("runtime.json"),
                temp.resolve("mode.lua"),
                temp.resolve("data"),
                temp.resolve("agreement.json"),
                temp.resolve("user_token.lua"),
                temp.resolve("video_upload.lua"));
        AgreementController ctrl = new AgreementController(new AgreementService(paths));
        HandlerMethod hm = new HandlerMethod(ctrl, AgreementController.class.getMethod("getAgreement"));
        assertTrue(in.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), hm));
    }

    @Test
    void optionsAllowedWhenNotInitialized(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, false);
        InitializationGateInterceptor in = interceptor(temp);
        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("OPTIONS");
        assertTrue(in.preHandle(req, new MockHttpServletResponse(), hm));
    }

    @Test
    void arbitraryControllerBlockedWithNotInitialized(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, false);
        InitializationGateInterceptor in = interceptor(temp);

        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(in.preHandle(new MockHttpServletRequest(), res, hm));
        assertEquals(503, res.getStatus());
        assertTrue(res.getContentAsString().contains("not_initialized"));
    }

    @Test
    void arbitraryControllerBlockedWhileInitializing(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, false);
        InitializationAccessGate gate = gate(temp);
        gate.beginInitExecution();
        try {
            InitializationGateInterceptor in = new InitializationGateInterceptor(gate);
            HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
            MockHttpServletResponse res = new MockHttpServletResponse();
            assertFalse(in.preHandle(new MockHttpServletRequest(), res, hm));
            assertEquals(503, res.getStatus());
            assertTrue(res.getContentAsString().contains("initializing"));
        } finally {
            gate.endInitExecution();
        }
    }

    @Test
    void arbitraryControllerAllowedWhenInitialized(@TempDir Path temp) throws Exception {
        writeRuntimeJson(temp, true);
        InitializationGateInterceptor in = interceptor(temp);

        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        assertTrue(in.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), hm));
    }

    private static void writeRuntimeJson(Path temp, boolean complete) throws Exception {
        Files.writeString(
                temp.resolve("runtime.json"),
                "{\"initialization_completed\": " + complete + "}\n");
    }

    private static InitializationAccessGate gate(Path temp) {
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("runtime.json"),
                temp.resolve("mode.lua"),
                temp.resolve("data"),
                temp.resolve("agreement.json"),
                temp.resolve("user_token.lua"),
                temp.resolve("video_upload.lua"));
        return new InitializationAccessGate(paths);
    }

    private static InitializationGateInterceptor interceptor(Path temp) {
        return new InitializationGateInterceptor(gate(temp));
    }

    private static HandlerMethod handlerMethod(Object bean, String methodName) throws Exception {
        Method m = bean.getClass().getMethod(methodName);
        return new HandlerMethod(bean, m);
    }

    @RestController
    static class SampleApiController {
        @GetMapping("/sample-ping")
        public String ping() {
            return "pong";
        }
    }
}
