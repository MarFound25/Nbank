#!/bin/bash

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функции для логирования
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Функция очистки при выходе
cleanup() {
    log_info "Остановка тестового окружения..."
    docker compose -f docker-compose-e2e.yml down
    if [ $? -eq 0 ]; then
        log_success "Окружение остановлено"
    else
        log_error "Ошибка при остановке окружения"
    fi
}

# Перехват сигналов для гарантированной очистки
trap cleanup EXIT INT TERM

# Главный скрипт
main() {
    echo "=========================================="
    echo "    ЗАПУСК API И UI ТЕСТОВ"
    echo "=========================================="
    echo ""

    # Шаг 1: Проверка наличия docker-compose файла
    if [ ! -f "docker-compose-e2e.yml" ]; then
        log_error "Файл docker-compose-e2e.yml не найден!"
        exit 1
    fi

    # Шаг 2: Поднятие тестового окружения
    log_info "Поднятие тестового окружения (docker-compose-e2e.yml)..."
    docker compose -f docker-compose-e2e.yml up -d

    if [ $? -ne 0 ]; then
        log_error "Не удалось поднять окружение"
        exit 1
    fi
    log_success "Окружение поднято"

    # Шаг 3: Ожидание готовности сервисов
    log_info "Ожидание готовности сервисов (30 секунд)..."
    sleep 30

    # Проверка доступности сервисов
    log_info "Проверка доступности сервисов..."

    # Проверка backend (порт 4111 для UI окружения)
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:4111/actuator/health | grep -q "200"; then
        log_success "Backend доступен (http://localhost:4111)"
    else
        log_warning "Backend не отвечает, но продолжаем..."
    fi

    # Проверка nginx (frontend через прокси)
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:80 | grep -q "200\|301\|302"; then
        log_success "Nginx доступен (http://localhost:80)"
    else
        log_warning "Nginx не отвечает, но продолжаем..."
    fi

    # Проверка selenoid (порт 4445)
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:4445/status | grep -q "200"; then
        log_success "Selenoid доступен (http://localhost:4445)"
    else
        log_warning "Selenoid не отвечает, но продолжаем..."
    fi

    echo ""

    # Шаг 4: Запуск тестов в контейнере
    log_info "Запуск API и UI тестов..."

    # Определяем имя образа для тестов (можно переопределить через переменную)
    TEST_IMAGE="${TEST_IMAGE:-nbank-tests:latest}"

    docker run --rm \
        --network nbank-network \
        -e APIBASEURL="http://backend:4111" \
        -e UIBASEURL="http://frontend:80" \
        -e SELENOID_URL="http://selenoid:4444" \
        -e SELENOID_UI_URL="http://selenoid-ui:8080" \
        $TEST_IMAGE

    TEST_EXIT_CODE=$?

    echo ""
    if [ $TEST_EXIT_CODE -eq 0 ]; then
        log_success "Все тесты успешно пройдены!"
    else
        log_error "Тесты завершились с ошибкой (код: $TEST_EXIT_CODE)"
    fi

    # Шаг 5: Очистка (вызовется через trap)
    log_info "Завершение работы..."

    exit $TEST_EXIT_CODE
}

# Запуск основной функции
main