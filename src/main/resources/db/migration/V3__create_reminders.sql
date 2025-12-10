-- Создание таблицы reminders со всеми полями
CREATE TABLE vectoros.reminders (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    repeatability VARCHAR(32) NOT NULL,
    reminder_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    next_reminder_time TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminder_user FOREIGN KEY (user_id)
        REFERENCES vectoros.users (id)
        ON DELETE CASCADE
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_reminders_user ON vectoros.reminders (user_id);
CREATE INDEX idx_reminders_time ON vectoros.reminders (reminder_time);
CREATE INDEX idx_reminders_status ON vectoros.reminders (status);
CREATE INDEX idx_reminders_next_time ON vectoros.reminders (next_reminder_time) 
    WHERE status = 'PENDING';

