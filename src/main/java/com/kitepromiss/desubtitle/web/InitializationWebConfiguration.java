package com.kitepromiss.desubtitle.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InitializationWebConfiguration implements WebMvcConfigurer {

    private final InitializationGateInterceptor initializationGateInterceptor;
    private final AnonymousUserBearerInterceptor anonymousUserBearerInterceptor;

    public InitializationWebConfiguration(
            InitializationGateInterceptor initializationGateInterceptor,
            AnonymousUserBearerInterceptor anonymousUserBearerInterceptor) {
        this.initializationGateInterceptor = initializationGateInterceptor;
        this.anonymousUserBearerInterceptor = anonymousUserBearerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(initializationGateInterceptor).order(Ordered.HIGHEST_PRECEDENCE);
        registry.addInterceptor(anonymousUserBearerInterceptor).order(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
