package com.kitepromiss.desubtitle.init;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class InitExecutionMutexTest {

    @Test
    void otherThreadCannotAcquireWhileHeld() throws Exception {
        InitExecutionMutex m = new InitExecutionMutex();
        assertTrue(m.tryAcquireExclusive());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> second = pool.submit(m::tryAcquireExclusive);
            assertFalse(second.get(5, TimeUnit.SECONDS));
            m.releaseExclusive();
            Future<Boolean> third = pool.submit(() -> {
                boolean ok = m.tryAcquireExclusive();
                if (ok) {
                    m.releaseExclusive();
                }
                return ok;
            });
            assertTrue(third.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }
}
