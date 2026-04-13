package com.kitepromiss.desubtitle.init;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/**
 * 保证全进程至多一个 {@link InitService} 初始化临界区在执行；与「初始化门禁」拦截器正交，仅约束 {@code POST /init} 互斥。
 */
@Component
public class InitExecutionMutex {

    private final ReentrantLock lock = new ReentrantLock();

    public boolean tryAcquireExclusive() {
        return lock.tryLock();
    }

    public void releaseExclusive() {
        lock.unlock();
    }
}
