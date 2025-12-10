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
    
    echo "⏳ Ждем 5 секунд, чтобы туннель полностью инициализировался..."
    sleep 5
    
    echo "🔄 Обновляю webhook на: ${webhook_url}"
    
    # Пробуем несколько раз с задержкой, так как туннель может быть еще не готов
    max_attempts=3
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        response=$(curl -s "${TELEGRAM_API_URL}/setWebhook?url=${webhook_url}")
        
        if echo "$response" | grep -q '"ok":true'; then
            echo "✅ Webhook успешно обновлен с попытки #$attempt!"
            echo "📝 Ответ: $response"
            return 0
        else
            echo "⚠️ Попытка #$attempt не удалась: $response"
            if [ $attempt -lt $max_attempts ]; then
                echo "⏳ Ждем еще 3 секунды и пробуем снова..."
                sleep 3
            fi
        fi
        attempt=$((attempt + 1))
    done
    
    echo "❌ Не удалось обновить webhook после $max_attempts попыток"
    return 1
}

# Файл для отслеживания обновления webhook
WEBHOOK_UPDATED_FILE="/tmp/cloudflared_webhook_updated.txt"

# Запускаем cloudflared и перехватываем его вывод
# Используем временный файл для избежания проблем с subshell
/usr/local/bin/cloudflared tunnel --url http://localhost:8080 2>&1 | tee /tmp/cloudflared_output.log | while IFS= read -r line || [ -n "$line" ]; do
    # Выводим строки cloudflared в stdout (пойдут в journal)
    echo "$line"
    
    # Ищем строку с URL туннеля
    if echo "$line" | grep -q "trycloudflare.com"; then
        # Извлекаем URL из строки
        url=$(echo "$line" | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | head -1)
        
        if [ ! -z "$url" ] && [ ! -f "$WEBHOOK_UPDATED_FILE" ]; then
            echo "🌐 Найден URL туннеля: $url"
            update_webhook "$url"
            # Сохраняем URL и флаг обновления
            echo "$url" > /tmp/cloudflared_url.txt
            touch "$WEBHOOK_UPDATED_FILE"
        fi
    fi
done

