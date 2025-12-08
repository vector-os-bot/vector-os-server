#!/bin/bash

# === 1. Получаем текущий ngrok URL ===
NGROK_URL=$(curl -s http://127.0.0.1:4040/api/tunnels | jq -r '.tunnels[0].public_url')

if [[ "$NGROK_URL" == "null" || -z "$NGROK_URL" ]]; then
  echo "❌ Ngrok URL не найден. Ngrok точно запущен?"
  exit 1
fi

echo "🔗 Текущий ngrok URL: $NGROK_URL"

# === 2. Вызываем Spring endpoint ===
RESULT=$(curl -s -X POST "http://localhost:8080/internal/updateWebhook?url=$NGROK_URL")

echo "✅ Ответ сервера: $RESULT"
echo "🎉 Webhook успешно обновлён!"
