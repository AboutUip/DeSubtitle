package com.kitepromiss.desubtitle.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kitepromiss.desubtitle.init.InitializationAccessGate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 未完成初始化时禁止访问除引导相关控制器外的 Spring MVC 控制器；初始化执行中同样禁止。白名单见
 * {@link MvcPublicEndpointRules#allowsWithoutInitialization(Object)}。
 */
@Component
public class InitializationGateInterceptor implements HandlerInterceptor {

    private final InitializationAccessGate gate;

    public InitializationGateInterceptor(InitializationAccessGate gate) {
        this.gate = gate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (MvcPublicEndpointRules.allowsWithoutInitialization(hm.getBean())) {
            return true;
        }
        if (gate.isInitializationComplete()) {
            return true;
        }
        if (gate.isInitExecutionInProgress()) {
            writeJson503(response, "initializing");
            return false;
        }
        writeJson503(response, "not_initialized");
        return false;
    }

    private static void writeJson503(HttpServletResponse response, String errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + errorCode + "\"}");
    }
}
