package com.vectoros.server.task.dto;

import com.vectoros.server.task.entity.Priority;
import com.vectoros.server.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO для создания и обновления задачи через API
 */
@Data
public class TaskDto {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private TaskStatus status; // Если не указан, будет NEW по умолчанию

    private LocalDate taskDate;

    private Priority priority; // Если не указан, будет MEDIUM по умолчанию

    private Instant deadlineAt;
}

