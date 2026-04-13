package com.kitepromiss.desubtitle.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 可选跨域：默认不启用（前端与 API 同机同端口由 {@code web/} 静态托管时无需 CORS）。独立前端开发服务器（如 Vite）连后端时，配置
 * {@code desubtitle.cors.allowed-origins} 为逗号分隔来源列表，并须带 {@code Authorization} 的场景下配合拦截器对 {@code OPTIONS} 放行。
 */
@Configuration
public class DesubtitleCorsConfiguration implements WebMvcConfigurer {

    @Value("${desubtitle.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        if (origins.isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
