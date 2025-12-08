package com.vectoros.server.telegram.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private final RestTemplate restTemplate;
    private final String botToken;

    public TelegramService(@Value("${spring.telegram.bot.token}") String botToken) {
        this.restTemplate = new RestTemplate();
        this.botToken = botToken;
        
        // Проверка токена при инициализации
        if (botToken == null || botToken.isEmpty()) {
            throw new IllegalStateException("Telegram bot token is not configured! Please check application.yml");
        }
        System.out.println("✅ Telegram bot token loaded successfully (length: " + botToken.length() + ")");
    }

    public void sendMessage(Long chatId, String text) {
        if (botToken == null || botToken.isEmpty()) {
            System.err.println("❌ Bot token is not set! Cannot send message.");
            return;
        }
        
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                System.err.println("❌ 401 Unauthorized - Проверьте правильность токена бота в application.yml");
                System.err.println("Токен начинается с: " + (botToken != null && botToken.length() > 10 
                    ? botToken.substring(0, 10) + "..." : "null"));
            }
            throw e;
        }
    }

    public void setWebhook(String webhookUrl) {
        String url = "https://api.telegram.org/bot" + botToken + "/setWebhook?url=" + webhookUrl;
        restTemplate.getForObject(url, String.class);
    }

    /**
     * Устанавливает постоянную кнопку меню "Открыть кабинет" для всех пользователей бота
     * Эта кнопка будет постоянно отображаться слева от поля ввода текста
     * @param webAppUrl URL веб-приложения кабинета
     */
    public void setMenuButton(String webAppUrl) {
        String url = "https://api.telegram.org/bot" + botToken + "/setChatMenuButton";
        
        Map<String, Object> menuButton = new HashMap<>();
        menuButton.put("type", "web_app");
        menuButton.put("text", "Открыть кабинет");
        
        Map<String, String> webApp = new HashMap<>();
        webApp.put("url", webAppUrl);
        menuButton.put("web_app", webApp);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("menu_button", menuButton);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ Menu button set successfully: " + response);
        } catch (Exception e) {
            System.err.println("❌ Error setting menu button: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Устанавливает постоянную кнопку меню для конкретного пользователя
     * @param chatId ID чата пользователя
     * @param webAppUrl URL веб-приложения кабинета
     */
    public void setMenuButtonForUser(Long chatId, String webAppUrl) {
        String url = "https://api.telegram.org/bot" + botToken + "/setChatMenuButton";
        
        Map<String, Object> menuButton = new HashMap<>();
        menuButton.put("type", "web_app");
        menuButton.put("text", "Открыть кабинет");
        
        Map<String, String> webApp = new HashMap<>();
        webApp.put("url", webAppUrl);
        menuButton.put("web_app", webApp);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("menu_button", menuButton);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ Menu button set for user " + chatId + ": " + response);
        } catch (Exception e) {
            System.err.println("❌ Error setting menu button for user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
