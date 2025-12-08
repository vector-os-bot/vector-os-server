CREATE TABLE users
(
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);
