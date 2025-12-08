package com.vectoros.server.telegram.controller;

import com.vectoros.server.telegram.commands.CommandDispatcher;
import com.vectoros.server.telegram.commands.CommandHandler;
import com.vectoros.server.telegram.service.TelegramService;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/webhook")
public class TelegramWebhookController {

    private final TelegramService telegramService;
    private final CommandDispatcher commandDispatcher;

    @PostMapping
    public ResponseEntity<String> onUpdateReceived(@RequestBody Map<String, Object> update) {

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return ResponseEntity.ok("OK");

        Map<String, Object> from = (Map<String, Object>) message.get("from");
        Long telegramId = ((Number) from.get("id")).longValue();
        String text = (String) message.get("text");

        // ищем хендлер
        CommandHandler handler = commandDispatcher.findHandler(text);

        if (handler != null) {
            handler.handle(telegramId, text);
        } else {
            telegramService.sendMessage(telegramId, "Я пока не знаю такую команду 😅");
        }

        return ResponseEntity.ok("OK");
    }

}
