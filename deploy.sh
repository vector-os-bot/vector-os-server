#!/bin/bash

set -e

echo "🚀 Starting deployment of VectorOS Server..."

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка наличия .env файла
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found. Creating from template...${NC}"
    cat > .env << EOF
# Database
POSTGRES_DB=vectoros
POSTGRES_USER=postgres
POSTGRES_PASSWORD=CHANGE_ME_IN_PRODUCTION

# Redis
REDIS_PASSWORD=

# Telegram Bot
TELEGRAM_BOT_TOKEN=YOUR_BOT_TOKEN_HERE
TELEGRAM_WEBAPP_URL=https://yourdomain.com/cabinet

# Server
SERVER_PORT=8080
EOF
    echo -e "${RED}❌ Please edit .env file with your actual values before deploying!${NC}"
    exit 1
fi

# Загрузка переменных окружения
set -a
source .env
set +a

echo -e "${GREEN}✅ Environment variables loaded${NC}"

# Остановка старых контейнеров (если есть)
echo "🛑 Stopping existing containers..."
docker compose -f docker-compose.prod.yml down || true

# Сборка образа приложения
echo "🔨 Building application image..."
docker compose -f docker-compose.prod.yml build --no-cache app

# Запуск сервисов
echo "🚀 Starting services..."
docker compose -f docker-compose.prod.yml up -d

# Ожидание готовности сервисов
echo "⏳ Waiting for services to be ready..."
sleep 10

# Проверка статуса
echo "📊 Service status:"
docker compose -f docker-compose.prod.yml ps

echo -e "${GREEN}✅ Deployment completed!${NC}"
echo ""
echo "📝 Useful commands:"
echo "  View logs:        docker compose -f docker-compose.prod.yml logs -f"
echo "  Stop services:    docker compose -f docker-compose.prod.yml down"
echo "  Restart app:      docker compose -f docker-compose.prod.yml restart app"
echo "  Check status:     docker compose -f docker-compose.prod.yml ps"

