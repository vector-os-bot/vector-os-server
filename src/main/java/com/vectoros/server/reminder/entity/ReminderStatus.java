package com.vectoros.server.reminder.entity;

public enum ReminderStatus {
    PENDING,    // Ожидает отправки
    SENT,       // Успешно отправлено
    FAILED,     // Не удалось отправить (после всех retry)
    CANCELLED   // Отменено пользователем
}

