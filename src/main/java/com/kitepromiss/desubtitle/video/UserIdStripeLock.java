package com.kitepromiss.desubtitle.video;

import org.springframework.stereotype.Component;

/**
 * 按 JWT {@code sub}（userId）哈希分条的进程内锁，用于同一用户对「去字幕」与「主动撤销视频」等互斥，避免与批量处理交错导致状态错乱。
 */
@Component
public class UserIdStripeLock {

    private static final int STRIPE_COUNT = 64;
    private final Object[] stripes = new Object[STRIPE_COUNT];

    public UserIdStripeLock() {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripes[i] = new Object();
        }
    }

    /** 与 {@link SendToDeSubtitleService}、{@link VideoUploadService} 中 stripe 数量一致，仅锁对象数组独立。 */
    public Object lockFor(String userId) {
        return stripes[Math.floorMod(userId.hashCode(), STRIPE_COUNT)];
    }
}
