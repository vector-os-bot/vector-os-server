-- Создание схемы vectoros
CREATE SCHEMA IF NOT EXISTS vectoros;

-- Предоставляем права пользователю базы данных
GRANT ALL ON SCHEMA vectoros TO CURRENT_USER;

-- Создание таблицы users
CREATE TABLE vectoros.users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(255),
    username VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска по telegram_id
CREATE INDEX idx_users_telegram_id ON vectoros.users (telegram_id);

