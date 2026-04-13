package com.kitepromiss.desubtitle.sqlite;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * 进程内对同一 SQLite 库（{@code data/desubtitle.db}）的互斥门闩：所有 JDBC/JPA 访问须在本组件的 {@link #supply} / {@link #run} 临界区内执行，避免多线程并发写导致 database locked。
 */
@Component
public class SqliteConcurrencyController {

    private final ReentrantLock lock = new ReentrantLock();

    public void run(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public <T> T supply(Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public <T> T call(Callable<T> callable) throws Exception {
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }
}
