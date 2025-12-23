package com.vectoros.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins:*}")
    private String allowedOrigins;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Добавляем префикс /api только для REST API контроллеров
        // Исключаем telegram.controller (webhook, internal) и actuator endpoints
        configurer.addPathPrefix("/api", 
            clazz -> {
                String packageName = clazz.getPackageName();
                // Применяем /api только к контроллерам в пакетах task.controllers и reminder.controllers
                return (packageName.contains("task.controllers") 
                    || packageName.contains("reminder.controllers"));
            }
        );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = "*".equals(allowedOrigins) 
            ? new String[]{"*"} 
            : allowedOrigins.split(",");
        
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(!allowedOrigins.equals("*"))
                .maxAge(3600);
    }
}

