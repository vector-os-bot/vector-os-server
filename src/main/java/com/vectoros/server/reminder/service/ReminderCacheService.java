package com.vectoros.server.reminder.service;

import com.vectoros.server.reminder.entity.ReminderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Сервис для кеширования активных напоминаний в Redis
 * Использует Sorted Set для хранения напоминаний, отсортированных по времени
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderCacheService {

    private static final String REDIS_KEY = "reminders:active";

    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${reminder.scheduler.redis.cache-days-ahead:7}")
    private int cacheDaysAhead;

    /**
     * Добавляет напоминание в Redis кеш, если оно в пределах кешируемого периода
     */
    public void addReminderToCache(ReminderEntity reminder) {
        try {
            if (shouldCache(reminder)) {
                ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
                double score = reminder.getNextReminderTime().toEpochMilli();
                zSetOps.add(REDIS_KEY, String.valueOf(reminder.getId()), score);
                log.debug("Added reminder {} to Redis cache with score {}", reminder.getId(), score);
            }
        } catch (Exception e) {
            log.warn("Failed to add reminder {} to Redis cache: {}. Will use PostgreSQL fallback.", 
                reminder.getId(), e.getMessage());
        }
    }

    /**
     * Обновляет напоминание в кеше
     */
    public void updateReminderInCache(ReminderEntity reminder) {
        try {
            // Удаляем старое значение
            redisTemplate.opsForZSet().remove(REDIS_KEY, String.valueOf(reminder.getId()));
            
            // Добавляем новое, если нужно
            if (shouldCache(reminder)) {
                addReminderToCache(reminder);
            }
        } catch (Exception e) {
            log.warn("Failed to update reminder {} in Redis cache: {}", reminder.getId(), e.getMessage());
        }
    }

    /**
     * Удаляет напоминание из кеша
     */
    public void removeReminderFromCache(Long reminderId) {
        try {
            redisTemplate.opsForZSet().remove(REDIS_KEY, String.valueOf(reminderId));
            log.debug("Removed reminder {} from Redis cache", reminderId);
        } catch (Exception e) {
            log.warn("Failed to remove reminder {} from Redis cache: {}", reminderId, e.getMessage());
        }
    }

    /**
     * Получает ID напоминаний, время которых наступило (до текущего момента)
     */
    public Set<String> getDueReminderIds() {
        try {
            long now = Instant.now().toEpochMilli();
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            return zSetOps.rangeByScore(REDIS_KEY, 0, now);
        } catch (Exception e) {
            log.warn("Failed to get due reminders from Redis: {}. Will use PostgreSQL fallback.", e.getMessage());
            return Set.of(); // Возвращаем пустой set, чтобы не ломать логику
        }
    }

    /**
     * Проверяет, нужно ли кешировать напоминание
     */
    private boolean shouldCache(ReminderEntity reminder) {
        if (reminder.getNextReminderTime() == null) {
            return false;
        }
        
        Instant cacheUntil = Instant.now().plus(cacheDaysAhead, ChronoUnit.DAYS);
        return reminder.getNextReminderTime().isBefore(cacheUntil) 
            && reminder.getNextReminderTime().isAfter(Instant.now().minus(1, ChronoUnit.HOURS));
    }

    /**
     * Очищает старые напоминания из кеша (вызывается периодически для очистки)
     */
    public void cleanupOldReminders() {
        try {
            long oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli();
            redisTemplate.opsForZSet().removeRangeByScore(REDIS_KEY, 0, oneHourAgo);
            log.debug("Cleaned up old reminders from cache (before {})", oneHourAgo);
        } catch (Exception e) {
            log.warn("Failed to cleanup old reminders from Redis: {}", e.getMessage());
        }
    }
}

