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

Рекомендуется разместить в `/opt` (стандартное место для приложений):

```bash
cd /opt
sudo git clone https://github.com/vector-os-bot/vector-os-server.git
cd vector-os-server
sudo chown -R $USER:$USER .  # Дать права текущему пользователю
```

**Альтернативные варианты:**
- `/srv/vectoros-server` - если планируете использовать systemd services
- `~/apps/vectoros-server` - если тестируете/разрабатываете (без sudo)

### 3. Настройка переменных окружения

Создайте файл `.env`:

```bash
cp .env.example .env  # если есть пример
nano .env
```

Установите следующие переменные:

**Вариант 1: Генерация надежных паролей (рекомендуется)**

```bash
# Сгенерировать пароль для PostgreSQL (24 символа)
openssl rand -base64 24

# Сгенерировать пароль для Redis (32 символа)
openssl rand -base64 32
```

**Вариант 2: Ручной ввод**

```env
# Database
POSTGRES_DB=vectoros
POSTGRES_USER=postgres
POSTGRES_PASSWORD=ВАШ_НАДЕЖНЫЙ_ПАРОЛЬ_МИНИМУМ_16_СИМВОЛОВ

# Redis
# Для production рекомендуется установить пароль для безопасности
# Можно оставить пустым для тестирования, но это небезопасно!
REDIS_PASSWORD=ваш_пароль_для_redis_или_оставить_пустым

# Telegram Bot (ОБЯЗАТЕЛЬНО!)
TELEGRAM_BOT_TOKEN=ваш_токен_от_BotFather

# URL веб-приложения, открывающегося при нажатии "Открыть кабинет"
# Если UI еще не готов, укажите заглушку или ngrok URL
# Примеры:
# - https://yourdomain.com/cabinet (когда будет готов домен)
# - https://your-ngrok-url.ngrok.io/cabinet (для тестов с ngrok)
# - https://yourdomain.com/cabinet (можно оставить как есть, если UI пока нет)
TELEGRAM_WEBAPP_URL=https://yourdomain.com/cabinet

# Server
SERVER_PORT=8080
```

**Примечания:**
- `POSTGRES_PASSWORD` - **ОБЯЗАТЕЛЬНО** задать надежный пароль (минимум 16 символов)
- `REDIS_PASSWORD` - можно оставить пустым для тестов, но для production лучше установить
- `TELEGRAM_WEBAPP_URL` - это URL, который открывается при нажатии кнопки в Telegram. Если UI еще нет, можно указать любой (замените позже)

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

## Настройка Webhook для Telegram

Telegram требует HTTPS для webhook. Есть несколько вариантов:

### Вариант 1: Cloudflare Tunnel (рекомендуется, работает в РФ)

**Преимущества:** Бесплатно, работает в РФ, не требует домена, автоматический HTTPS

```bash
# 1. Установка cloudflared на сервере
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/

# 2. Запуск туннеля (один раз, чтобы получить URL)
cloudflared tunnel --url http://localhost:8080

# Скопируй HTTPS URL (например: https://abc123.trycloudflare.com)
```

Затем настрой webhook:
```bash
curl "http://localhost:8080/internal/updateWebhook?url=https://ТВОЙ_CLOUDFLARE_URL"
```

**Для постоянной работы с автоматическим обновлением webhook:**

Используй скрипт, который автоматически обновляет Telegram webhook при каждом запуске:

```bash
# 1. Скопируй скрипт на сервер (если еще не скопирован)
# Скрипт уже в репозитории: scripts/cloudflared-with-webhook.sh

# 2. Сделай скрипт исполняемым
chmod +x /opt/vector-os-server/scripts/cloudflared-with-webhook.sh

# 3. Создай systemd сервис
sudo nano /etc/systemd/system/cloudflared.service
```

Содержимое файла:
```ini
[Unit]
Description=Cloudflare Tunnel with Auto Webhook Update
After=network.target

[Service]
Type=simple
User=root
ExecStart=/opt/vector-os-server/scripts/cloudflared-with-webhook.sh
EnvironmentFile=/opt/vector-os-server/.env
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Затем:
```bash
sudo systemctl daemon-reload
sudo systemctl enable cloudflared
sudo systemctl start cloudflared
sudo systemctl status cloudflared

# Посмотри логи для проверки
sudo journalctl -u cloudflared -f
```

**Важно:** Скрипт автоматически найдет новый URL при каждом запуске и обновит webhook в Telegram. Токен бота берется из `.env` файла (переменная `TELEGRAM_BOT_TOKEN`).

### Вариант 2: Домен + Nginx + Let's Encrypt (production)

См. раздел "Настройка Nginx" ниже.

### Вариант 3: Локальный туннель (альтернатива ngrok)

**localtunnel** (не требует регистрации):
```bash
# Установка
npm install -g localtunnel

# Запуск
lt --port 8080

# Скопируй HTTPS URL и используй для webhook
```

**serveo.net** (SSH туннель, бесплатный):
```bash
ssh -R 80:localhost:8080 serveo.net
# Получишь URL вида: https://abc123.serveo.net
```

После настройки webhook проверь:
```bash
curl "https://api.telegram.org/botВАШ_ТОКЕН/getWebhookInfo"
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

