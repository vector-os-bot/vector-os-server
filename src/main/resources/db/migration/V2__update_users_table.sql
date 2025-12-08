-- 1) Разрешаем NULL для first_name и username
ALTER TABLE users
    ALTER COLUMN first_name DROP NOT NULL,
ALTER COLUMN username DROP NOT NULL;

-- 2) Добавляем updated_at
ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- 3) Добавляем статус пользователя
-- Возможные значения: 'new', 'active', 'blocked'
ALTER TABLE users
    ADD COLUMN status VARCHAR(20) DEFAULT 'new';

-- 4) Для существующих записей
UPDATE users
SET status = 'new',
    updated_at = NOW()
WHERE status IS NULL;
