package com.vectoros.server.reminder.repository;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.entity.ReminderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    List<ReminderEntity> findByUserId(Long userId);
    
    List<ReminderEntity> findByStatusAndNextReminderTimeLessThanEqual(
        ReminderStatus status, Instant time);
    
    List<ReminderEntity> findByStatusAndLastAttemptAtLessThan(
        ReminderStatus status, Instant time);
    
    /**
     * Загружает напоминание с User (EAGER fetch) для избежания LazyInitializationException
     */
    @Query("SELECT r FROM ReminderEntity r JOIN FETCH r.user WHERE r.id = :id")
    Optional<ReminderEntity> findByIdWithUser(@Param("id") Long id);
}

