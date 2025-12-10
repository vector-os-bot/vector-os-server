#!/bin/bash

# Скрипт для автоматического обновления Telegram webhook при запуске cloudflared
# Использование: ./cloudflared-with-webhook.sh
# Токен берется из переменной окружения TELEGRAM_BOT_TOKEN или из .env файла

# Загружаем токен из .env файла, если он существует (из директории проекта)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | grep TELEGRAM_BOT_TOKEN | xargs)
fi

# Используем переменную окружения или значение по умолчанию
TELEGRAM_BOT_TOKEN="${TELEGRAM_BOT_TOKEN:-8559625460:AAGnvHa6JdZYQZb_w7z4Rbesw31xYFZ2wcY}"
TELEGRAM_API_URL="https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}"

# Функция для обновления webhook
update_webhook() {
    local url=$1
    local webhook_url="${url}/webhook"
    
    echo "🔄 Обновляю webhook на: ${webhook_url}"
    
    response=$(curl -s "${TELEGRAM_API_URL}/setWebhook?url=${webhook_url}")
    
    if echo "$response" | grep -q '"ok":true'; then
        echo "✅ Webhook успешно обновлен!"
        echo "📝 Ответ: $response"
    else
        echo "❌ Ошибка обновления webhook: $response"
    fi
}

# Запускаем cloudflared и перехватываем его вывод
/usr/local/bin/cloudflared tunnel --url http://localhost:8080 2>&1 | while IFS= read -r line; do
    # Выводим строки cloudflared в лог
    echo "$line"
    
    # Ищем строку с URL туннеля (может быть в разных форматах)
    if echo "$line" | grep -q "trycloudflare.com"; then
        # Извлекаем URL из строки (используем extended regex)
        url=$(echo "$line" | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | head -1)
        
        if [ ! -z "$url" ]; then
            echo "🌐 Найден URL туннеля: $url"
            update_webhook "$url"
            # Сохраняем URL в файл для последующего использования
            echo "$url" > /tmp/cloudflared_url.txt
        fi
    fi
    
    # Также проверяем формат "Visit it at"
    if echo "$line" | grep -q "Visit it at"; then
        url=$(echo "$line" | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | head -1)
        if [ ! -z "$url" ]; then
            echo "🌐 Найден URL из лога: $url"
            update_webhook "$url"
            echo "$url" > /tmp/cloudflared_url.txt
        fi
    fi
done

