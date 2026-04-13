package com.kitepromiss.desubtitle.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import com.kitepromiss.desubtitle.user.AnonymousUserPrincipal;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;

class AnonymousUserBearerInterceptorTest {

    @Test
    void missingHeaderReturns401() throws Exception {
        AnonymousUserBearerInterceptor in = new AnonymousUserBearerInterceptor(jwt -> Optional.empty());
        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(in.preHandle(new MockHttpServletRequest(), res, hm));
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("missing_bearer_token"));
    }

    @Test
    void optionsPreflightSkipsBearer() throws Exception {
        AnonymousUserBearerInterceptor in = new AnonymousUserBearerInterceptor(jwt -> Optional.empty());
        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("OPTIONS");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(in.preHandle(req, res, hm));
        assertEquals(200, res.getStatus());
    }

    @Test
    void validBearerSetsAttribute() throws Exception {
        AnonymousUserPrincipal p = new AnonymousUserPrincipal("user-z", "jti-z");
        AnonymousUserBearerInterceptor in = new AnonymousUserBearerInterceptor(jwt -> Optional.of(p));
        HandlerMethod hm = handlerMethod(new SampleApiController(), "ping");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer ok.jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(in.preHandle(req, res, hm));
        assertEquals("user-z", req.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID));
        assertEquals(p, req.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_PRINCIPAL));
    }

    private static HandlerMethod handlerMethod(Object bean, String methodName) throws Exception {
        Method m = bean.getClass().getMethod(methodName);
        return new HandlerMethod(bean, m);
    }

    @RestController
    static class SampleApiController {
        @GetMapping("/x")
        public String ping() {
            return "pong";
        }
    }
}
