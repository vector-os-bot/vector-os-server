package com.vectoros.server.task.mapper;

import com.vectoros.server.task.dto.TaskDto;
import com.vectoros.server.task.entity.Priority;
import com.vectoros.server.task.entity.TaskEntity;
import com.vectoros.server.task.entity.TaskStatus;
import com.vectoros.server.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper для конвертации между TaskDto и TaskEntity
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    /**
     * Конвертирует TaskDto в TaskEntity
     * User устанавливается отдельно, так как его нет в DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "priority", ignore = true)
    TaskEntity toEntity(TaskDto dto);

    /**
     * Конвертирует TaskDto в TaskEntity с установкой User и дефолтных значений
     */
    default TaskEntity toEntity(TaskDto dto, User user) {
        TaskEntity entity = toEntity(dto);
        entity.setUser(user);
        
        // Устанавливаем дефолтные значения, если они не указаны
        if (dto.getStatus() == null) {
            entity.setStatus(TaskStatus.NEW);
        } else {
            entity.setStatus(dto.getStatus());
        }
        
        if (dto.getPriority() == null) {
            entity.setPriority(Priority.MEDIUM);
        } else {
            entity.setPriority(dto.getPriority());
        }
        
        return entity;
    }
}

