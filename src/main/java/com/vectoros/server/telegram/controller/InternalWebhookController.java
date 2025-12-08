package com.vectoros.server.telegram.controller;

import com.vectoros.server.telegram.service.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class InternalWebhookController {

    private final TelegramService telegramService;

    public InternalWebhookController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping("/updateWebhook")
    public ResponseEntity<String> updateWebhook(@RequestParam String url) {

        // Делаем полный корректный путь
        String fullUrl = url + "/webhook";

        telegramService.setWebhook(fullUrl);

        return ResponseEntity.ok("Webhook updated to: " + fullUrl);
    }
}
