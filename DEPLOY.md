# Инструкция по деплою VectorOS Server

## Требования

- Docker и Docker Compose установлены на сервере
- Открыт порт 8080 (или другой указанный в переменных окружения)
- Минимум 2GB RAM
- Минимум 10GB свободного места на диске

## Быстрый старт

### 1. Подготовка сервера

```bash
# Обновление системы (для Ubuntu/Debian)
sudo apt update && sudo apt upgrade -y

# Установка Docker (если не установлен)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Установка Docker Compose (если не установлен)
sudo apt install docker-compose -y
# или для новой версии:
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Клонирование репозитория на сервер

```bash
cd /opt
git clone https://github.com/vector-os-bot/vector-os-server.git
cd vector-os-server
```

### 3. Настройка переменных окружения

Создайте файл `.env`:

```bash
cp .env.example .env  # если есть пример
nano .env
```

Установите следующие переменные:

```env
# Database
POSTGRES_DB=vectoros
POSTGRES_USER=postgres
POSTGRES_PASSWORD=ВАШ_НАДЕЖНЫЙ_ПАРОЛЬ_ДЛЯ_БД

# Redis (можно оставить пустым для локального использования)
REDIS_PASSWORD=

# Telegram Bot (ОБЯЗАТЕЛЬНО!)
TELEGRAM_BOT_TOKEN=ваш_токен_от_BotFather
TELEGRAM_WEBAPP_URL=https://yourdomain.com/cabinet

# Server
SERVER_PORT=8080
```

### 4. Деплой

```bash
# Сделать скрипт исполняемым
chmod +x deploy.sh

# Запустить деплой
./deploy.sh
```

## Альтернативный способ (без скрипта)

```bash
# Сборка и запуск
docker compose -f docker-compose.prod.yml up -d --build
```

## Проверка работы

```bash
# Просмотр логов
docker compose -f docker-compose.prod.yml logs -f app

# Проверка статуса
docker compose -f docker-compose.prod.yml ps

# Проверка здоровья приложения
curl http://localhost:8080/actuator/health
```

## Обновление приложения

```bash
# Получить последние изменения
git pull origin main

# Пересобрать и перезапустить
docker compose -f docker-compose.prod.yml up -d --build app
```

## Настройка Nginx (опционально, для домена)

Создайте конфиг `/etc/nginx/sites-available/vectoros`:

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Активируйте:

```bash
sudo ln -s /etc/nginx/sites-available/vectoros /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Настройка SSL через Let's Encrypt

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

## Полезные команды

```bash
# Остановка всех сервисов
docker compose -f docker-compose.prod.yml down

# Остановка с удалением данных (ОСТОРОЖНО!)
docker compose -f docker-compose.prod.yml down -v

# Перезапуск только приложения
docker compose -f docker-compose.prod.yml restart app

# Просмотр логов конкретного сервиса
docker compose -f docker-compose.prod.yml logs -f postgres
docker compose -f docker-compose.prod.yml logs -f redis
docker compose -f docker-compose.prod.yml logs -f app

# Подключение к БД
docker exec -it vectoros_postgres_prod psql -U postgres -d vectoros

# Бэкап БД
docker exec vectoros_postgres_prod pg_dump -U postgres vectoros > backup.sql

# Восстановление БД
docker exec -i vectoros_postgres_prod psql -U postgres vectoros < backup.sql
```

## Резервное копирование

Рекомендуется настроить автоматическое резервное копирование:

```bash
# Создайте скрипт backup.sh
#!/bin/bash
BACKUP_DIR="/backups/vectoros"
mkdir -p $BACKUP_DIR
DATE=$(date +%Y%m%d_%H%M%S)

# Бэкап БД
docker exec vectoros_postgres_prod pg_dump -U postgres vectoros | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# Удаление старых бэкапов (старше 7 дней)
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete
```

Добавьте в crontab:

```bash
0 2 * * * /path/to/backup.sh
```

## Мониторинг

Для мониторинга можно использовать:

- **cAdvisor** - мониторинг контейнеров
- **Prometheus + Grafana** - метрики и дашборды
- **Docker stats** - простой мониторинг ресурсов

```bash
# Просмотр использования ресурсов
docker stats
```

## Решение проблем

### Приложение не запускается

```bash
# Проверьте логи
docker compose -f docker-compose.prod.yml logs app

# Проверьте переменные окружения
docker compose -f docker-compose.prod.yml config
```

### База данных не подключена

```bash
# Проверьте статус PostgreSQL
docker compose -f docker-compose.prod.yml ps postgres

# Проверьте логи PostgreSQL
docker compose -f docker-compose.prod.yml logs postgres
```

### Redis недоступен

```bash
# Проверьте статус Redis
docker compose -f docker-compose.prod.yml ps redis

# Тест подключения к Redis
docker exec -it vectoros_redis_prod redis-cli ping
```

## Безопасность

⚠️ **ВАЖНО для продакшена:**

1. Измените все пароли по умолчанию в `.env`
2. Используйте сильные пароли для PostgreSQL
3. Настройте файрвол (закройте порты 5432 и 6379 от внешнего доступа)
4. Настройте SSL/TLS для приложения
5. Регулярно обновляйте Docker образы
6. Настройте резервное копирование

```bash
# Пример настройки UFW (Ubuntu)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # Приложение (или только localhost)
sudo ufw enable
```

