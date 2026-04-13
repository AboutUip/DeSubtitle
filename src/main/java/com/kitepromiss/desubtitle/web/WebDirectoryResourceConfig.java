package com.kitepromiss.desubtitle.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * 自仓库根目录 {@code web/} 提供静态资源与单页入口；未命中文件时对无扩展名的路径回退 {@code index.html}。
 * 运行工作目录须为模块根（或显式存在 {@code web} 目录）。REST API 建议统一前缀 {@code /api}，以免与前端路由冲突。
 */
@Configuration
public class WebDirectoryResourceConfig implements WebMvcConfigurer {

    private final String webLocation;

    public WebDirectoryResourceConfig() {
        Path web = Path.of("web").toAbsolutePath().normalize();
        if (!Files.isDirectory(web)) {
            throw new IllegalStateException("静态资源目录不存在: " + web + "（请在模块根目录启动或调整工作目录）");
        }
        String uri = web.toUri().toString();
        this.webLocation = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // add-mappings=false 时无默认 welcome，显式将 / 指到静态 index.html
        registry.addRedirectViewController("/", "/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(Ordered.LOWEST_PRECEDENCE);
        registry.addResourceHandler("/**")
                .addResourceLocations(webLocation)
                .resourceChain(true)
                .addResolver(new SpaIndexFallbackResolver());
    }

    private static final class SpaIndexFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null && resource.exists()) {
                return resource;
            }
            String path = resourcePath == null ? "" : resourcePath;
            if (shouldSpaFallback(path)) {
                Resource index = location.createRelative("index.html");
                if (index.exists()) {
                    return index;
                }
            }
            return null;
        }

        private static boolean shouldSpaFallback(String path) {
            if (path.isEmpty()) {
                return true;
            }
            int slash = path.lastIndexOf('/');
            String last = slash >= 0 ? path.substring(slash + 1) : path;
            return !last.contains(".");
        }
    }
}
