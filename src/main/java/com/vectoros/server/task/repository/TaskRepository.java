package com.vectoros.server.task.repository;

import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByUserId(Long userId);
    List<TaskEntity> findByStatus(TaskStatus status);
}
