package com.vectoros.server.reminder.service;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.entity.ReminderRepeatability;
import com.vectoros.server.reminder.entity.ReminderStatus;
import com.vectoros.server.reminder.repository.ReminderRepository;
import com.vectoros.server.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис для планирования и отправки напоминаний
 * Использует гибридный подход: быстрый слой (Redis) + медленный слой (PostgreSQL fallback)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private final ReminderRepository reminderRepository;
    private final ReminderCacheService reminderCacheService;
    private final TelegramService telegramService;
    private final ReminderRepeatabilityCalculator repeatabilityCalculator;

    @Value("${reminder.scheduler.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${reminder.scheduler.retry.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    /**
     * Быстрый слой: проверка Redis каждые 30 секунд
     * Обрабатывает напоминания, которые в ближайшие 7 дней
     */
    @Scheduled(fixedDelayString = "${reminder.scheduler.redis.check-interval-seconds:30}000")
    public void processRemindersFromRedis() {
        try {
            Set<String> dueReminderIds = reminderCacheService.getDueReminderIds();
            
            if (dueReminderIds.isEmpty()) {
                log.trace("No reminders due from Redis cache at {}", Instant.now());
                return;
            }

            log.info("Found {} reminders due from Redis cache at {}", dueReminderIds.size(), Instant.now());

            for (String reminderIdStr : dueReminderIds) {
                try {
                    Long reminderId = Long.parseLong(reminderIdStr);
                    Optional<ReminderEntity> reminderOpt = reminderRepository.findById(reminderId);
                    
                    if (reminderOpt.isPresent()) {
                        ReminderEntity reminder = reminderOpt.get();
                        
                        // Двойная проверка времени (на случай если время изменилось в БД)
                        Instant now = Instant.now();
                        if (reminder.getNextReminderTime() != null 
                            && reminder.getNextReminderTime().isBefore(now)
                            && reminder.getStatus() == ReminderStatus.PENDING) {
                            
                            log.info("Processing reminder ID: {} from Redis. Next time: {}, Now: {}", 
                                reminder.getId(), reminder.getNextReminderTime(), now);
                            processReminder(reminder);
                        } else {
                            log.debug("Reminder ID: {} not ready. Next time: {}, Now: {}, Status: {}", 
                                reminder.getId(), reminder.getNextReminderTime(), now, reminder.getStatus());
                        }
                    } else {
                        // Напоминание удалено из БД, удаляем из кеша
                        reminderCacheService.removeReminderFromCache(reminderId);
                    }
                } catch (Exception e) {
                    log.error("Error processing reminder {} from Redis: {}", reminderIdStr, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in processRemindersFromRedis: {}", e.getMessage(), e);
        }
    }

    /**
     * Медленный слой: проверка PostgreSQL каждые 5 минут (fallback)
     * Обрабатывает все активные напоминания, включая те, что могли пропустить Redis
     */
    @Scheduled(fixedDelayString = "${reminder.scheduler.postgres.check-interval-minutes:5}00000")
    public void processRemindersFromPostgres() {
        try {
            Instant now = Instant.now();
            List<ReminderEntity> dueReminders = reminderRepository.findByStatusAndNextReminderTimeLessThanEqual(
                ReminderStatus.PENDING, now);

            if (dueReminders.isEmpty()) {
                log.trace("No reminders due from PostgreSQL at {}", now);
                return;
            }

            log.info("Found {} reminders due from PostgreSQL (fallback check) at {}", dueReminders.size(), now);

            for (ReminderEntity reminder : dueReminders) {
                try {
                    processReminder(reminder);
                } catch (Exception e) {
                    log.error("Error processing reminder {} from PostgreSQL: {}", reminder.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in processRemindersFromPostgres: {}", e.getMessage(), e);
        }
    }

    /**
     * Очистка старых напоминаний из Redis кеша (раз в час)
     */
    @Scheduled(fixedRateString = "3600000") // 1 час
    public void cleanupCache() {
        reminderCacheService.cleanupOldReminders();
    }

    /**
     * Recovery: при старте приложения проверяем и обрабатываем "зависшие" напоминания
     */
    @Scheduled(initialDelay = 30000, fixedRate = Long.MAX_VALUE) // Запускается один раз через 30 секунд после старта
    public void recoveryOnStartup() {
        log.info("Starting reminder recovery check...");
        Instant now = Instant.now();
        
        // Находим все напоминания со статусом PENDING, время которых уже прошло
        List<ReminderEntity> stuckReminders = reminderRepository
            .findByStatusAndNextReminderTimeLessThanEqual(ReminderStatus.PENDING, now);
        
        if (!stuckReminders.isEmpty()) {
            log.warn("Found {} stuck reminders, attempting recovery", stuckReminders.size());
            for (ReminderEntity reminder : stuckReminders) {
                try {
                    // Добавляем в кеш если нужно, потом обрабатываем
                    reminderCacheService.addReminderToCache(reminder);
                    processReminder(reminder);
                } catch (Exception e) {
                    log.error("Error recovering reminder {}: {}", reminder.getId(), e.getMessage(), e);
                }
            }
        }
        
        log.info("Recovery check completed");
    }

    /**
     * Обрабатывает одно напоминание: отправляет и обновляет статус
     */
    @Transactional
    public void processReminder(ReminderEntity reminder) {
        log.info("Processing reminder ID: {}, title: {}", reminder.getId(), reminder.getTitle());
        
        // Перезагружаем reminder с JOIN FETCH User, чтобы избежать LazyInitializationException
        // Это загружает User в той же транзакции и в том же запросе
        ReminderEntity loadedReminder = reminderRepository.findByIdWithUser(reminder.getId())
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminder.getId()));
        
        loadedReminder.setLastAttemptAt(Instant.now());
        
        // Теперь можем безопасно получить telegramId, так как User загружен через JOIN FETCH
        Long telegramId = loadedReminder.getUser().getTelegramId();
        if (telegramId == null) {
            log.error("TelegramId is null for reminder ID: {}", loadedReminder.getId());
            handleFailedDelivery(loadedReminder);
            reminderRepository.save(loadedReminder);
            return;
        }
        
        try {
            // Отправляем напоминание
            String message = buildReminderMessage(loadedReminder);
            telegramService.sendMessage(telegramId, message);
            
            // Успешная отправка
            loadedReminder.setRetryCount(0);
            handleSuccessfulDelivery(loadedReminder);
            
            log.info("Successfully sent reminder ID: {}", loadedReminder.getId());
            
        } catch (Exception e) {
            log.error("Failed to send reminder ID: {}: {}", loadedReminder.getId(), e.getMessage());
            handleFailedDelivery(loadedReminder);
        }
        
        reminderRepository.save(loadedReminder);
    }

    /**
     * Обрабатывает успешную доставку
     */
    private void handleSuccessfulDelivery(ReminderEntity reminder) {
        // Удаляем из кеша
        reminderCacheService.removeReminderFromCache(reminder.getId());
        
        // Проверяем repeatability
        Instant nextTime = repeatabilityCalculator.calculateNextReminderTime(
            reminder.getReminderTime(), reminder.getRepeatability());
        
        if (nextTime != null && reminder.getRepeatability() != ReminderRepeatability.ONCE) {
            // Повторяющееся напоминание - планируем следующее
            reminder.setNextReminderTime(nextTime);
            reminder.setStatus(ReminderStatus.PENDING);
            reminder.setReminderTime(nextTime); // Обновляем базовое время
            
            // Добавляем в кеш
            reminderCacheService.addReminderToCache(reminder);
            
            log.info("Scheduled next reminder for ID: {} at {}", reminder.getId(), nextTime);
        } else {
            // Одноразовое напоминание - помечаем как отправленное
            reminder.setStatus(ReminderStatus.SENT);
        }
    }

    /**
     * Обрабатывает неудачную доставку (retry механизм)
     */
    private void handleFailedDelivery(ReminderEntity reminder) {
        reminder.setRetryCount(reminder.getRetryCount() + 1);
        
        if (reminder.getRetryCount() >= maxRetryAttempts) {
            // Превышен лимит попыток
            reminder.setStatus(ReminderStatus.FAILED);
            reminderCacheService.removeReminderFromCache(reminder.getId());
            log.warn("Reminder ID: {} marked as FAILED after {} attempts", 
                reminder.getId(), reminder.getRetryCount());
        } else {
            // Планируем retry через N минут
            Instant retryTime = Instant.now().plusSeconds(retryDelayMinutes * 60L);
            reminder.setNextReminderTime(retryTime);
            
            // Обновляем в кеше (удаляем старое, добавляем новое)
            reminderCacheService.removeReminderFromCache(reminder.getId());
            reminderCacheService.addReminderToCache(reminder);
            
            log.info("Scheduled retry #{} for reminder ID: {} at {}. Cache updated: {}", 
                reminder.getRetryCount(), reminder.getId(), retryTime, 
                reminderCacheService.getDueReminderIds().contains(String.valueOf(reminder.getId())));
        }
    }

    /**
     * Формирует сообщение для отправки пользователю
     * Метод не обращается к User, чтобы избежать LazyInitializationException
     */
    private String buildReminderMessage(ReminderEntity reminder) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 Напоминание: ").append(reminder.getTitle());
        
        if (reminder.getDescription() != null && !reminder.getDescription().isEmpty()) {
            message.append("\n\n").append(reminder.getDescription());
        }
        
        message.append("\n\n📅 Тип: ").append(reminder.getType());
        
        if (reminder.getRepeatability() != ReminderRepeatability.ONCE) {
            message.append("\n🔄 Повтор: ").append(getRepeatabilityText(reminder.getRepeatability()));
        }
        
        return message.toString();
    }

    private String getRepeatabilityText(ReminderRepeatability repeatability) {
        return switch (repeatability) {
            case EVERY_MINUTE -> "Каждую минуту";
            case HOURLY -> "Ежечасно";
            case DAILY -> "Ежедневно";
            case WEEKLY -> "Еженедельно";
            case MONTHLY -> "Ежемесячно";
            case YEARLY -> "Ежегодно";
            default -> "Одноразово";
        };
    }
}

