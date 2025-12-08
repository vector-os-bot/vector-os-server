package com.vectoros.server.telegram.commands;

import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.service.TaskService;
import com.vectoros.server.telegram.service.TelegramService;
import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
public class CreateTaskCommandHandler implements CommandHandler {

    private final TaskService taskService;
    private final UserService userService; // чтобы найти пользователя по telegramId
    private final TelegramService telegramService;

    @Override
    public void handle(Long telegramId, String text) {
        String title = text.substring("/createTask ".length());
        User user = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setUser(user);
        taskService.createTask(task);

        telegramService.sendMessage(telegramId, "Задача создана: " + title);
    }

    @Override
    public String getCommand() {
        return "/create";
    }

}

