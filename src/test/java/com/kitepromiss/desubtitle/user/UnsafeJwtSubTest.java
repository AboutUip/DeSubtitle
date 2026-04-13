package com.kitepromiss.desubtitle.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class UnsafeJwtSubTest {

    @Test
    void parsesSubFromCompactJwtPayload() {
        String payloadJson = "{\"sub\":\"user-abc-1\"}";
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        String jwt = "x." + b64 + ".sig";
        Optional<String> sub = UnsafeJwtSub.tryParseSub(jwt);
        assertTrue(sub.isPresent());
        assertEquals("user-abc-1", sub.get());
    }

    @Test
    void emptyWhenMalformed() {
        assertTrue(UnsafeJwtSub.tryParseSub("not-a-jwt").isEmpty());
        assertTrue(UnsafeJwtSub.tryParseSub("").isEmpty());
    }
}
