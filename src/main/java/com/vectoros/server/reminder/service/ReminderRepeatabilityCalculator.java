package com.vectoros.server.reminder.service;

import com.vectoros.server.reminder.entity.ReminderRepeatability;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class ReminderRepeatabilityCalculator {

    /**
     * Вычисляет следующее время напоминания на основе repeatability
     */
    public Instant calculateNextReminderTime(Instant currentReminderTime, ReminderRepeatability repeatability) {
        if (repeatability == ReminderRepeatability.ONCE) {
            return null; // Одноразовое напоминание, следующего не будет
        }

        Instant now = Instant.now();
        Instant nextTime = currentReminderTime;

        // Если текущее время уже прошло, начинаем отсчет от текущего момента
        if (currentReminderTime.isBefore(now)) {
            nextTime = now;
        }

        switch (repeatability) {
            case EVERY_MINUTE:
                return nextTime.plus(1, ChronoUnit.MINUTES);
            case HOURLY:
                return nextTime.plus(1, ChronoUnit.HOURS);
            case DAILY:
                return nextTime.plus(1, ChronoUnit.DAYS);
            case WEEKLY:
                return nextTime.plus(7, ChronoUnit.DAYS);
            case MONTHLY:
                return nextTime.plus(30, ChronoUnit.DAYS);
            case YEARLY:
                return nextTime.plus(365, ChronoUnit.DAYS);
            default:
                return null;
        }
    }
}

