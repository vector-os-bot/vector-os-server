package com.vectoros.server.telegram.commands;

import com.vectoros.server.user.service.UserService;
import com.vectoros.server.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements CommandHandler {

    private final TelegramService telegramService;
    private final UserService userService;

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public void handle(Long chatId, String text) {

        // создаём пользователя при первом запуске
        userService.findOrCreate(chatId);

        telegramService.sendMessage(chatId,
                "🔥 Привет! Добро пожаловать в VectorOS.\n" +
                        "Это твой персональный помощник для задач, привычек, финансов и многого другого.\n\n" +
                        "Напиши /help, чтобы посмотреть, что я умею."
        );
    }
}
