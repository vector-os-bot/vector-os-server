CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,

                       title VARCHAR(255) NOT NULL,
                       description TEXT,

                       status VARCHAR(32) NOT NULL DEFAULT 'NEW',

                       task_date DATE,

                       priority VARCHAR(32),

                       deadline_at TIMESTAMP,

                       user_id BIGINT NOT NULL,
                       CONSTRAINT fk_task_user FOREIGN KEY (user_id)
                           REFERENCES users (id)
                           ON DELETE CASCADE,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индексы
CREATE INDEX idx_tasks_user ON tasks (user_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_deadline ON tasks (deadline_at);
