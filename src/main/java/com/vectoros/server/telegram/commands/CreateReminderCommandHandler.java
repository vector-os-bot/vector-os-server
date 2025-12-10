package com.vectoros.server.telegram.commands;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.entity.ReminderRepeatability;
import com.vectoros.server.reminder.entity.ReminderType;
import com.vectoros.server.reminder.service.ReminderService;
import com.vectoros.server.telegram.service.TelegramService;
import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@AllArgsConstructor
@Component
public class CreateReminderCommandHandler implements CommandHandler {

    private final ReminderService reminderService;
    private final UserService userService;
    private final TelegramService telegramService;

    @Override
    public void handle(Long telegramId, String text) {
        try {
            // Парсим команду: /createReminder EVERY_MINUTE title text here
            String commandPrefix = "/createReminder ";
            if (!text.startsWith(commandPrefix)) {
                telegramService.sendMessage(telegramId, "❌ Неверный формат команды. Используйте: /createReminder EVERY_MINUTE название напоминания");
                return;
            }

            String args = text.substring(commandPrefix.length()).trim();
            if (args.isEmpty()) {
                telegramService.sendMessage(telegramId, "❌ Укажите тип повторяемости и название. Пример: /createReminder EVERY_MINUTE Пить воду");
                return;
            }

            // Разделяем на части
            String[] parts = args.split("\\s+", 2);
            if (parts.length < 2) {
                telegramService.sendMessage(telegramId, "❌ Укажите тип повторяемости и название. Пример: /createReminder EVERY_MINUTE Пить воду");
                return;
            }

            String repeatabilityStr = parts[0].toUpperCase();
            String title = parts[1];

            // Парсим тип повторяемости
            ReminderRepeatability repeatability;
            try {
                repeatability = ReminderRepeatability.valueOf(repeatabilityStr);
            } catch (IllegalArgumentException e) {
                telegramService.sendMessage(telegramId, 
                    "❌ Неверный тип повторяемости. Доступные: ONCE, EVERY_MINUTE, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY");
                return;
            }

            // Находим пользователя
            User user = userService.findByTelegramId(telegramId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Создаем напоминание
            ReminderEntity reminder = new ReminderEntity();
            reminder.setTitle(title);
            reminder.setUser(user);
            reminder.setType(ReminderType.TASK); // Дефолтный тип
            reminder.setRepeatability(repeatability);
            
            // Устанавливаем reminder_time на текущее время + 1 минута (чтобы сразу начать)
            reminder.setReminderTime(Instant.now().plusSeconds(60));
            
            ReminderEntity saved = reminderService.createReminder(reminder);

            String repeatabilityText = getRepeatabilityText(repeatability);
            telegramService.sendMessage(telegramId, 
                "✅ Напоминание создано!\n" +
                "📝 Название: " + title + "\n" +
                "🔄 Повтор: " + repeatabilityText + "\n" +
                "⏰ Первое напоминание: через 1 минуту");

        } catch (Exception e) {
            log.error("Error creating reminder: {}", e.getMessage(), e);
            telegramService.sendMessage(telegramId, "❌ Ошибка при создании напоминания: " + e.getMessage());
        }
    }

    @Override
    public String getCommand() {
        return "/createReminder";
    }

    private String getRepeatabilityText(ReminderRepeatability repeatability) {
        return switch (repeatability) {
            case ONCE -> "Одноразово";
            case EVERY_MINUTE -> "Каждую минуту";
            case HOURLY -> "Ежечасно";
            case DAILY -> "Ежедневно";
            case WEEKLY -> "Еженедельно";
            case MONTHLY -> "Ежемесячно";
            case YEARLY -> "Ежегодно";
        };
    }
}

