package com.vectoros.server.task.service;

import com.vectoros.server.task.dto.TaskDto;
import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.mapper.TaskMapper;
import com.vectoros.server.task.repository.TaskRepository;
import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;
    private final TaskMapper taskMapper;

    /**
     * Создание задачи через Entity (для внутреннего использования, например, Telegram commands)
     */
    public TaskEntity createTask(TaskEntity task) {
        return taskRepository.save(task);
    }

    /**
     * Создание задачи через DTO и telegramId (для REST API)
     * Использует TaskMapper для конвертации DTO в Entity
     * Пользователь должен быть зарегистрирован (через /start в боте)
     */
    @Transactional
    public TaskEntity createTask(TaskDto dto, Long telegramId) {
        User user = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found with telegramId: " + telegramId + ". Please register via /start command first."));

        // Конвертируем DTO в Entity через mapper
        TaskEntity task = taskMapper.toEntity(dto, user);

        return taskRepository.save(task);
    }

    public List<TaskEntity> getAllUserTasks(Long telegramId) {
        final Optional<User> user = userService.findByTelegramId(telegramId);
        if (user.isPresent()) {
            return taskRepository.findByUserId(user.get().getId());
        }

        return List.of();
    }
}

