#!/bin/bash

# Выбор окружения: api или ui (по умолчанию api)
ENV="${1:-api}"

echo ">>> Используется окружение: $ENV"

# Остановка любого текущего окружения
echo ">>> Остановить Docker Compose"
docker compose down 2>/dev/null || true

if [ "$ENV" = "ui" ]; then
    # UI окружение (с фронтом и Selenoid)
    COMPOSE_FILE="docker-compose-e2e.yml"

    echo ">>> Docker pull всех образов браузеров"

    # Путь до файла
    json_file="./config/browsers.json"

    # Проверяем, что jq установлен
    if ! command -v jq &> /dev/null; then
        echo "❌ jq is not installed. Please install jq and try again."
        exit 1
    fi

    # Извлекаем все значения .image через jq
    images=$(jq -r '.. | objects | select(.image) | .image' "$json_file")

    # Пробегаем по каждому образу и выполняем docker pull
    for image in $images; do
        echo "Pulling $image..."
        docker pull "$image"
    done

    echo ">>> Запуск UI окружения (docker-compose-e2e.yml)"
    docker compose -f "$COMPOSE_FILE" up -d

else
    # API окружение (с БД и WireMock)
    COMPOSE_FILE="docker-compose.yml"

    echo ">>> Запуск API окружения (docker-compose.yml)"
    docker compose -f "$COMPOSE_FILE" up -d
fi

echo ""
echo "✅ Окружение поднято!"
echo ""
echo "📌 Для API (порт 4112): http://localhost:4112"
echo "📌 Для UI (порт 80): http://localhost:80"
echo "📌 Selenoid: http://localhost:4444"
echo ""
echo "➡️  Для остановки: docker compose down"