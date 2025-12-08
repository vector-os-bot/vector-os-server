package com.vectoros.server.task.service;

import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.repository.TaskRepository;
import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.repository.UserRepository;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskEntity createTask(TaskEntity task) {
        return taskRepository.save(task); // тут Hibernate вставляет запись в базу
    }

    public List<TaskEntity> getAllUserTasks(Long telegramId) {
        final Optional<User> user = userService.findByTelegramId(telegramId);
        if (user.isPresent()) {
            return taskRepository.findByUserId(user.get().getId());
        }

        return List.of();
    }
}

