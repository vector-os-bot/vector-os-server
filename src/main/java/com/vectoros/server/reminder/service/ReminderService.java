package com.vectoros.server.reminder.service;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.entity.ReminderStatus;
import com.vectoros.server.reminder.repository.ReminderRepository;
import com.vectoros.server.user.entity.User;
import com.vectoros.server.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserService userService;
    private final ReminderCacheService reminderCacheService;

    @Transactional
    public ReminderEntity createReminder(ReminderEntity reminder) {
        // Устанавливаем начальные значения
        if (reminder.getStatus() == null) {
            reminder.setStatus(ReminderStatus.PENDING);
        }
        if (reminder.getNextReminderTime() == null) {
            reminder.setNextReminderTime(reminder.getReminderTime());
        }
        if (reminder.getRetryCount() == null) {
            reminder.setRetryCount(0);
        }

        ReminderEntity saved = reminderRepository.save(reminder);
        
        // Добавляем в Redis кеш если напоминание в ближайшие 7 дней
        reminderCacheService.addReminderToCache(saved);
        
        log.info("Created reminder ID: {}, next reminder time: {}", saved.getId(), saved.getNextReminderTime());
        return saved;
    }

    public List<ReminderEntity> getAllUserReminders(Long telegramId) {
        final Optional<User> user = userService.findByTelegramId(telegramId);
        if (user.isPresent()) {
            return reminderRepository.findByUserId(user.get().getId());
        }

        return List.of();
    }

    public Optional<ReminderEntity> getReminderById(Long reminderId) {
        return reminderRepository.findById(reminderId);
    }

    @Transactional
    public ReminderEntity updateReminder(ReminderEntity reminder) {
        ReminderEntity updated = reminderRepository.save(reminder);
        reminderCacheService.updateReminderInCache(updated);
        return updated;
    }
}

