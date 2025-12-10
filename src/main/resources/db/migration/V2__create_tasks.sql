-- Создание таблицы tasks
CREATE TABLE vectoros.tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    task_date DATE,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    deadline_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_user FOREIGN KEY (user_id)
        REFERENCES vectoros.users (id)
        ON DELETE CASCADE
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_tasks_user ON vectoros.tasks (user_id);
CREATE INDEX idx_tasks_status ON vectoros.tasks (status);
CREATE INDEX idx_tasks_deadline ON vectoros.tasks (deadline_at);

