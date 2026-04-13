package com.kitepromiss.desubtitle.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kitepromiss.desubtitle.api.IndicatorController;
import com.kitepromiss.desubtitle.api.InitController;
import com.kitepromiss.desubtitle.api.LifeController;
import com.kitepromiss.desubtitle.api.UserTokenController;
import com.kitepromiss.desubtitle.indicator.IndicatorSnapshotService;

class MvcPublicEndpointRulesTest {

    @Test
    void lifeAllowedBeforeInitAndExemptFromGlobalBearerInterceptor() {
        LifeController c = new LifeController(null, null, null, null, null, 3);
        assertTrue(MvcPublicEndpointRules.allowsWithoutInitialization(c));
        assertTrue(MvcPublicEndpointRules.allowsWithoutBearer(c));
    }

    @Test
    void indicatorAllowedBeforeInitButRequiresBearerAfter() {
        IndicatorController c = new IndicatorController((IndicatorSnapshotService) null);
        assertTrue(MvcPublicEndpointRules.allowsWithoutInitialization(c));
        assertFalse(MvcPublicEndpointRules.allowsWithoutBearer(c));
    }

    @Test
    void getUserTokenAndInitExemptFromBearer() {
        assertTrue(MvcPublicEndpointRules.allowsWithoutBearer(new InitController(null)));
        assertTrue(MvcPublicEndpointRules.allowsWithoutBearer(new UserTokenController(null)));
    }

    @Test
    void arbitraryBeanNotPublic() {
        assertFalse(MvcPublicEndpointRules.allowsWithoutInitialization(new Object()));
        assertFalse(MvcPublicEndpointRules.allowsWithoutBearer(new Object()));
    }
}
