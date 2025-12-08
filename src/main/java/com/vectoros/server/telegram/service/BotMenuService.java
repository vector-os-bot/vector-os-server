package com.vectoros.server.telegram.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для отправки сообщений с inline кнопками
 * Используется для дополнительных функций, основная кнопка меню устанавливается через BotInitializer
 */
@Service
@RequiredArgsConstructor
public class BotMenuService {

    private final TelegramService telegramService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${spring.telegram.bot.token}")
    private String botToken;
    
    @Value("${spring.telegram.bot.webapp.url:https://yourdomain.com/cabinet}")
    private String webAppUrl;

    /**
     * Отправляет сообщение с inline кнопкой "Открыть кабинет"
     * @param chatId ID чата пользователя
     */
    public void sendCabinetButton(Long chatId) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", "Добро пожаловать! Открой кабинет ниже:");

        // Создаем inline кнопку
        Map<String, Object> button = new HashMap<>();
        button.put("text", "Открыть кабинет");
        Map<String, String> webApp = new HashMap<>();
        webApp.put("url", webAppUrl);
        button.put("web_app", webApp);

        Map<String, Object> inlineKeyboard = new HashMap<>();
        inlineKeyboard.put("inline_keyboard", List.of(List.of(button)));

        payload.put("reply_markup", inlineKeyboard);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("Ошибка при отправке сообщения с кнопкой: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
