package com.kitepromiss.desubtitle.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.user.AnonymousUserPrincipal;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.user.UserTokenManagementService;
import com.kitepromiss.desubtitle.user.UserTokenRepository;

class UserDataManagerTest {

    @Test
    void getStateDoesNotCreateEmptyPartitions() {
        UserDataManager m = manager();
        assertTrue(m.getState("u1", "domain", "k").isEmpty());
        assertTrue(m.snapshotDomain("u1", "domain").isEmpty());
        m.putState("u2", "domain", "k", 1);
        assertTrue(m.getState("u1", "domain", "k").isEmpty());
    }

    @Test
    void concurrentGetOrCreateIncrementsSharedCounter() throws Exception {
        UserDataManager m = manager();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 1000; j++) {
                        m.getOrCreateState("user-a", "cache", "hits", () -> new AtomicInteger(0)).incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();
        assertEquals(8000, m.getState("user-a", "cache", "hits", AtomicInteger.class).orElseThrow().get());
    }

    @Test
    void currentUserFromRequestContext() {
        UserDataManager m = manager();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID, "uid-9");
        req.setAttribute(
                AnonymousUserRequestAttributes.ANONYMOUS_USER_PRINCIPAL, new AnonymousUserPrincipal("uid-9", "jti-9"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertEquals("uid-9", m.requireUserId());
            m.putState("session", "x", 42);
            assertEquals(42, m.getState("session", "x", Integer.class).orElseThrow());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void requireUserIdWithoutContextFails() {
        RequestContextHolder.resetRequestAttributes();
        assertThrows(NoCurrentUserContextException.class, () -> manager().requireUserId());
    }

    private static UserDataManager manager() {
        return new UserDataManager(new UserTokenManagementService(new SqliteConcurrencyController(), noopRepo()));
    }

    @SuppressWarnings("unchecked")
    private static UserTokenRepository noopRepo() {
        return (UserTokenRepository) Proxy.newProxyInstance(
                UserTokenRepository.class.getClassLoader(),
                new Class<?>[] {UserTokenRepository.class},
                (p, m, a) -> {
                    Class<?> rt = m.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt.isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
    }
}
