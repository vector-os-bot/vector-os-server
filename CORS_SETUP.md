# Настройка CORS для Vector OS Server

## Проблема
При обращении к API с frontend возникает ошибка "Failed to fetch" из-за отсутствия настроек CORS.

## Решение

Добавь конфигурационный класс для CORS:

### Создай файл: `src/main/java/com/vectoros/server/config/WebConfig.java`

```java
package com.vectoros.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // В development разрешаем все источники
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
```

### Альтернативный способ - через аннотацию на контроллере:

Добавь `@CrossOrigin` аннотацию к контроллеру:

```java
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/tasks")
public class TaskController {
    // ...
}
```

## После настройки

1. Перезапусти Spring Boot приложение
2. Проверь, что запросы работают из браузера

## Для production

В production замени `allowedOrigins("*")` на конкретные домены:
```java
.allowedOrigins(
    "https://your-production-domain.com",
    "https://your-telegram-webapp-url.com"
)
```

