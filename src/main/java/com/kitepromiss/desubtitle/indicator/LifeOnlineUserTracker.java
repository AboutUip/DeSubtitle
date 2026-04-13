package com.kitepromiss.desubtitle.indicator;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 按 {@code GET /life} 成功处理次数维护「在线」用户数（量表 {@value #ONLINE_USERS_GAUGE}），不依赖其它接口上的 Bearer 有效 token。
 *
 * <p>统计的是<strong>近期成功 ping 过的匿名 userId 个数</strong>，非精确「人类」；多标签页若各持不同 JWT 仍会多计。Token
 * 失效后刷新会复用 {@link #revokePresence} 去掉旧 {@code sub}，避免同一人短时间被算两名。
 */
@Component
public class LifeOnlineUserTracker {

    public static final String ONLINE_USERS_GAUGE = "online_users";

    /** 超过该时间未再成功调用 {@code GET /life} 的用户不计入在线（约为前端 life 心跳周期的数倍）。 */
    private static final long PRESENCE_TTL_MS = 90_000L;

    private final ConcurrentHashMap<String, Long> lastLifePingMillis = new ConcurrentHashMap<>();
    private final IndicatorRecorder indicatorRecorder;

    public LifeOnlineUserTracker(IndicatorRecorder indicatorRecorder) {
        this.indicatorRecorder = indicatorRecorder;
    }

    /**
     * 在每次成功处理 {@code GET /life} 且已确定 {@code userId} 后调用（含提交 JWT 无效、刚刷新签发新用户的情形）。
     */
    public void recordLifePing(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        lastLifePingMillis.put(userId, now);
        pruneExpiredAndPublishGauge(now);
    }

    /**
     * 在因 JWT 失效而签发新匿名用户之前调用，移除旧 token payload 中的 {@code sub}，避免在线人数虚高。
     */
    public void revokePresence(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        lastLifePingMillis.remove(userId);
        pruneExpiredAndPublishGauge(System.currentTimeMillis());
    }

    private void pruneExpiredAndPublishGauge(long now) {
        long cutoff = now - PRESENCE_TTL_MS;
        lastLifePingMillis.entrySet().removeIf(e -> e.getValue() < cutoff);
        indicatorRecorder.setGauge(ONLINE_USERS_GAUGE, lastLifePingMillis.size());
    }
}
