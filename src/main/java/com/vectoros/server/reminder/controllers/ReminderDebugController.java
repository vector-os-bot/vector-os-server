package com.vectoros.server.reminder.controllers;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.entity.ReminderStatus;
import com.vectoros.server.reminder.repository.ReminderRepository;
import com.vectoros.server.reminder.service.ReminderCacheService;
import com.vectoros.server.reminder.service.ReminderSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug контроллер для диагностики работы системы напоминаний
 */
@RestController
@RequestMapping("/debug/reminders")
@RequiredArgsConstructor
public class ReminderDebugController {

    private final ReminderRepository reminderRepository;
    private final ReminderCacheService reminderCacheService;
    private final ReminderSchedulerService reminderSchedulerService;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> getReminderStatus(@PathVariable Long id) {
        Map<String, Object> status = new HashMap<>();
        
        // Получаем напоминание из БД
        ReminderEntity reminder = reminderRepository.findById(id).orElse(null);
        if (reminder == null) {
            status.put("error", "Reminder not found");
            return ResponseEntity.ok(status);
        }

        status.put("reminder", Map.of(
            "id", reminder.getId(),
            "title", reminder.getTitle(),
            "status", reminder.getStatus(),
            "nextReminderTime", reminder.getNextReminderTime(),
            "retryCount", reminder.getRetryCount(),
            "lastAttemptAt", reminder.getLastAttemptAt()
        ));

        // Проверяем в Redis
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Double score = zSetOps.score("reminders:active", String.valueOf(id));
        status.put("inRedis", score != null);
        status.put("redisScore", score != null ? Instant.ofEpochMilli(score.longValue()) : null);

        // Текущее время
        Instant now = Instant.now();
        status.put("currentTime", now);
        status.put("isDue", reminder.getNextReminderTime() != null && reminder.getNextReminderTime().isBefore(now));
        status.put("timeUntilDue", reminder.getNextReminderTime() != null 
            ? java.time.Duration.between(now, reminder.getNextReminderTime()).getSeconds() 
            : null);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/due")
    public ResponseEntity<Map<String, Object>> getDueReminders() {
        Instant now = Instant.now();
        
        List<ReminderEntity> dueReminders = reminderRepository
            .findByStatusAndNextReminderTimeLessThanEqual(ReminderStatus.PENDING, now);
        
        Set<String> dueFromRedis = reminderCacheService.getDueReminderIds();
        
        Map<String, Object> result = new HashMap<>();
        result.put("currentTime", now);
        result.put("dueFromPostgres", dueReminders.stream()
            .map(r -> Map.of("id", r.getId(), "title", r.getTitle(), 
                "nextReminderTime", r.getNextReminderTime()))
            .toList());
        result.put("dueFromRedis", dueFromRedis);
        result.put("countFromPostgres", dueReminders.size());
        result.put("countFromRedis", dueFromRedis.size());
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process/{id}")
    public ResponseEntity<String> processReminderManually(@PathVariable Long id) {
        ReminderEntity reminder = reminderRepository.findById(id).orElse(null);
        if (reminder == null) {
            return ResponseEntity.badRequest().body("Reminder not found");
        }

        try {
            reminderSchedulerService.processReminder(reminder);
            return ResponseEntity.ok("Reminder processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/add-to-cache/{id}")
    public ResponseEntity<String> addToCache(@PathVariable Long id) {
        ReminderEntity reminder = reminderRepository.findById(id).orElse(null);
        if (reminder == null) {
            return ResponseEntity.badRequest().body("Reminder not found");
        }

        reminderCacheService.addReminderToCache(reminder);
        return ResponseEntity.ok("Added to cache");
    }
}

