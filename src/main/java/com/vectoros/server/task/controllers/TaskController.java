package com.vectoros.server.task.controllers;

import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.service.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public TaskEntity createTask(@RequestBody TaskEntity task) {
        return taskService.createTask(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskEntity>> getAllUserTasks(@RequestParam Long telegramId) {
        return ResponseEntity.ok(taskService.getAllUserTasks(telegramId));
    }
}
