package com.kitepromiss.desubtitle.api;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.indicator.IndicatorSnapshotService;
import com.kitepromiss.desubtitle.indicator.LifeOnlineUserTracker;
import com.kitepromiss.desubtitle.user.AnonymousUserJwtGate;
import com.kitepromiss.desubtitle.user.AnonymousUserPrincipal;
import com.kitepromiss.desubtitle.user.JwtSecretNotConfiguredException;
import com.kitepromiss.desubtitle.user.JwtSigningSecretSource;
import com.kitepromiss.desubtitle.user.UnsafeJwtSub;
import com.kitepromiss.desubtitle.user.UserJwtIssuerService;
import com.kitepromiss.desubtitle.user.UserJwtSignatureSupport;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * 存活探测 + 提交 token 校验与按需刷新 + 内存指标快照。不经 Bearer 拦截器，但须显式携带 {@code Authorization: Bearer …}。
 */
@RestController
public class LifeController {

    private static final Pattern BEARER = Pattern.compile("^Bearer\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    private final AnonymousUserJwtGate anonymousUserJwtGate;
    private final UserJwtIssuerService userJwtIssuerService;
    private final IndicatorSnapshotService indicatorSnapshotService;
    private final JwtSigningSecretSource jwtSigningSecretSource;
    private final LifeOnlineUserTracker lifeOnlineUserTracker;
    private final int videoProcessingLanes;

    public LifeController(
            AnonymousUserJwtGate anonymousUserJwtGate,
            UserJwtIssuerService userJwtIssuerService,
            IndicatorSnapshotService indicatorSnapshotService,
            JwtSigningSecretSource jwtSigningSecretSource,
            LifeOnlineUserTracker lifeOnlineUserTracker,
            @Value("${desubtitle.ui.video-processing-lanes:3}") int videoProcessingLanes) {
        this.anonymousUserJwtGate = anonymousUserJwtGate;
        this.userJwtIssuerService = userJwtIssuerService;
        this.indicatorSnapshotService = indicatorSnapshotService;
        this.jwtSigningSecretSource = jwtSigningSecretSource;
        this.lifeOnlineUserTracker = lifeOnlineUserTracker;
        this.videoProcessingLanes = clampVideoProcessingLanes(videoProcessingLanes);
    }

    private static int clampVideoProcessingLanes(int n) {
        if (n < 1) {
            return 1;
        }
        if (n > 8) {
            return 8;
        }
        return n;
    }

    @GetMapping(value = "/life", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> life(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LifeErrorBody("missing_token"));
        }
        Matcher m = BEARER.matcher(authorization.trim());
        if (!m.matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LifeErrorBody("invalid_authorization_header"));
        }
        String submittedJwt = m.group(1);

        Optional<AnonymousUserPrincipal> principal = anonymousUserJwtGate.verifyCompactJwt(submittedJwt);
        if (principal.isPresent()) {
            String userId = principal.get().userId();
            lifeOnlineUserTracker.recordLifePing(userId);
            long remaining = remainingExpiresInSeconds(submittedJwt);
            LifeStatusPayload body = new LifeStatusPayload(
                    true,
                    true,
                    false,
                    submittedJwt,
                    remaining,
                    userId,
                    indicatorSnapshotService.combinedSnapshot(),
                    videoProcessingLanes);
            return ResponseEntity.ok(body);
        }

        try {
            UnsafeJwtSub.tryParseSub(submittedJwt).ifPresent(lifeOnlineUserTracker::revokePresence);
            UserJwtIssuerService.IssuedAnonymousToken issued = userJwtIssuerService.issueToken();
            lifeOnlineUserTracker.recordLifePing(issued.anonymousUserId());
            LifeStatusPayload body = new LifeStatusPayload(
                    true,
                    false,
                    true,
                    issued.token(),
                    issued.expiresInSeconds(),
                    issued.anonymousUserId(),
                    indicatorSnapshotService.combinedSnapshot(),
                    videoProcessingLanes);
            return ResponseEntity.ok(body);
        } catch (JwtSecretNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LifeErrorBody("jwt_secret_not_configured"));
        }
    }

    private long remainingExpiresInSeconds(String compactJwt) {
        try {
            SecretKey key = UserJwtSignatureSupport.hmacSha256Key(jwtSigningSecretSource);
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(compactJwt).getPayload();
            if (claims.getExpiration() == null) {
                return 0L;
            }
            long expMs = claims.getExpiration().getTime();
            return Math.max(0L, (expMs - System.currentTimeMillis()) / 1000L);
        } catch (JwtException | JwtSecretNotConfiguredException e) {
            return 0L;
        }
    }

    public record LifeErrorBody(String error) {}
}
