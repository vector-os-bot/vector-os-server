package com.vectoros.server.telegram.config;

import com.vectoros.server.telegram.service.TelegramService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramService telegramService;

    @Value("${spring.telegram.bot.webapp.url:https://yourdomain.com/cabinet}")
    private String webAppUrl;

    /**
     * Устанавливает постоянную кнопку меню "Открыть кабинет" при старте приложения
     * Эта кнопка будет видна всем пользователям бота постоянно
     */
    @PostConstruct
    public void initializeMenuButton() {
        try {
            telegramService.setMenuButton(webAppUrl);
            System.out.println("✅ Постоянная кнопка 'Открыть кабинет' успешно установлена");
        } catch (Exception e) {
            System.err.println("❌ Ошибка при установке кнопки меню: " + e.getMessage());
        }
    }
}

