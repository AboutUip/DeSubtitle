package com.kitepromiss.desubtitle.credential;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * {@code debug_mode=true} 时存放用户提交的 AccessKey，**不落库**；进程结束即丢失。
 */
@Component
public class InMemoryAliyunCredentialHolder {

    public record Pair(String accessKeyId, String accessKeySecret) {}

    private final AtomicReference<Pair> ref = new AtomicReference<>();

    public void set(String accessKeyId, String accessKeySecret) {
        ref.set(new Pair(accessKeyId, accessKeySecret));
    }

    public Optional<Pair> get() {
        return Optional.ofNullable(ref.get());
    }

    public void clear() {
        ref.set(null);
    }
}
