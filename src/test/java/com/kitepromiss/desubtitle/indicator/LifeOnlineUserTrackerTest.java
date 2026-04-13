package com.kitepromiss.desubtitle.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LifeOnlineUserTrackerTest {

    @Test
    void distinctUsersIncreaseOnlineGauge() {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        LifeOnlineUserTracker tracker = new LifeOnlineUserTracker(reg);
        tracker.recordLifePing("u1");
        tracker.recordLifePing("u2");
        assertEquals(2.0, reg.snapshot().gauges().get(LifeOnlineUserTracker.ONLINE_USERS_GAUGE), 0.001);
        tracker.recordLifePing("u1");
        assertEquals(2.0, reg.snapshot().gauges().get(LifeOnlineUserTracker.ONLINE_USERS_GAUGE), 0.001);
    }

    @Test
    void revokePresenceBeforeNewId_avoidsDoubleCountLikeTokenRefresh() {
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        LifeOnlineUserTracker tracker = new LifeOnlineUserTracker(reg);
        tracker.recordLifePing("old-user");
        assertEquals(1.0, reg.snapshot().gauges().get(LifeOnlineUserTracker.ONLINE_USERS_GAUGE), 0.001);
        tracker.revokePresence("old-user");
        tracker.recordLifePing("new-user");
        assertEquals(1.0, reg.snapshot().gauges().get(LifeOnlineUserTracker.ONLINE_USERS_GAUGE), 0.001);
    }
}
