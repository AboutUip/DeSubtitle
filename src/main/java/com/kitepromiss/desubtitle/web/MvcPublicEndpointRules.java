package com.kitepromiss.desubtitle.web;

import com.kitepromiss.desubtitle.api.AgreementController;
import com.kitepromiss.desubtitle.api.IndicatorController;
import com.kitepromiss.desubtitle.api.InitController;
import com.kitepromiss.desubtitle.api.InitCredentialController;
import com.kitepromiss.desubtitle.api.InitStatusController;
import com.kitepromiss.desubtitle.api.LifeController;
import com.kitepromiss.desubtitle.api.UserTokenController;

/**
 * 初始化门禁与 Bearer 门禁的判定入口：二者白名单<strong>不同</strong>——未完成初始化前可访问的接口多于免 Bearer 的接口。
 */
public final class MvcPublicEndpointRules {

    private MvcPublicEndpointRules() {}

    /**
     * 未完成初始化亦可访问的控制器（与 {@link InitializationGateInterceptor} 一致）。
     */
    public static boolean allowsWithoutInitialization(Object handlerBean) {
        if (handlerBean == null) {
            return false;
        }
        return handlerBean instanceof LifeController
                || handlerBean instanceof AgreementController
                || handlerBean instanceof IndicatorController
                || handlerBean instanceof UserTokenController
                || handlerBean instanceof InitController
                || handlerBean instanceof InitStatusController
                || handlerBean instanceof InitCredentialController;
    }

    /**
     * 无需全局 Bearer 拦截的控制器：{@code GET /life}（自行解析 {@code Authorization}）、{@code GET /getUserToken}、
     * {@code init} 引导三接口，以及 Spring Boot 错误控制器。其它控制器在通过初始化门禁后仍须由拦截器校验 JWT。
     */
    public static boolean allowsWithoutBearer(Object handlerBean) {
        if (handlerBean == null) {
            return false;
        }
        return handlerBean instanceof LifeController
                || handlerBean instanceof UserTokenController
                || handlerBean instanceof InitController
                || handlerBean instanceof InitStatusController
                || handlerBean instanceof InitCredentialController
                || isSpringBootErrorController(handlerBean);
    }

    /**
     * Spring Boot 3/4 中 {@code ErrorController} 接口包名不同，避免编译期绑定具体包。
     */
    private static boolean isSpringBootErrorController(Object bean) {
        if (bean == null) {
            return false;
        }
        return implementsInterfaceNamed(bean.getClass(), "org.springframework.boot.webmvc.error.ErrorController")
                || implementsInterfaceNamed(bean.getClass(), "org.springframework.boot.web.servlet.error.ErrorController");
    }

    private static boolean implementsInterfaceNamed(Class<?> type, String interfaceName) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> ifc : c.getInterfaces()) {
                if (interfaceName.equals(ifc.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
