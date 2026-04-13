package com.kitepromiss.desubtitle.config;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在实例化数据源之前创建 SQLite 文件路径的父目录（{@link SqliteJdbcFileSupport}）；对 {@code main} 与 {@code @SpringBootTest} 均生效。
 */
@Configuration(proxyBeanMethods = false)
public class SqliteDataDirectoryConfiguration {

    @Bean
    static BeanFactoryPostProcessor sqliteJdbcFileParentDirectoryInitializer(ConfigurableEnvironment environment) {
        return beanFactory -> {
            try {
                SqliteJdbcFileSupport.ensureParentDirectoryExists(environment.getProperty("spring.datasource.url"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
