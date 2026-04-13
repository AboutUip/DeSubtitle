package com.kitepromiss.desubtitle.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SqliteConcurrencyControllerTest {

    @Test
    void serializesConcurrentIncrements() throws Exception {
        SqliteConcurrencyController c = new SqliteConcurrencyController();
        AtomicInteger n = new AtomicInteger();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 1000; j++) {
                        c.run(() -> n.incrementAndGet());
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
        assertEquals(threads * 1000, n.get());
    }
}
