package com.kitepromiss.desubtitle.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kitepromiss.desubtitle.user.AnonymousUserJwtGate;
import com.kitepromiss.desubtitle.user.AnonymousUserPrincipal;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 除 {@link MvcPublicEndpointRules#allowsWithoutBearer(Object)} 外，要求 {@code Authorization: Bearer <JWT>}；校验走
 * {@link AnonymousUserJwtGate}（含 SQLite 行状态）。
 */
@Component
public class AnonymousUserBearerInterceptor implements HandlerInterceptor {

    private static final Pattern BEARER = Pattern.compile("^Bearer\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    private final AnonymousUserJwtGate anonymousUserJwtGate;

    public AnonymousUserBearerInterceptor(AnonymousUserJwtGate anonymousUserJwtGate) {
        this.anonymousUserJwtGate = anonymousUserJwtGate;
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
        if (MvcPublicEndpointRules.allowsWithoutBearer(hm.getBean())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            writeJson401(response, "missing_bearer_token");
            return false;
        }
        Matcher m = BEARER.matcher(auth.trim());
        if (!m.matches()) {
            writeJson401(response, "invalid_authorization_header");
            return false;
        }
        String jwt = m.group(1);
        var principal = anonymousUserJwtGate.verifyCompactJwt(jwt);
        if (principal.isPresent()) {
            AnonymousUserPrincipal p = principal.get();
            request.setAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_PRINCIPAL, p);
            request.setAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID, p.userId());
            return true;
        }
        writeJson401(response, "invalid_or_revoked_token");
        return false;
    }

    private static void writeJson401(HttpServletResponse response, String errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + errorCode + "\"}");
    }
}
