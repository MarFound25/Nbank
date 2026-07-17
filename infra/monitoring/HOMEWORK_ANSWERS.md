# Monitoring & Logging — доступ и ответы

## Сейчас открыто локально (port-forward уже запущен)

| Сервис | URL | Логин |
|--------|-----|--------|
| **Kibana** | http://localhost:5601 | без пароля |
| **Grafana** | http://localhost:3001 | `admin` / `admin` |
| **Prometheus** | http://localhost:9090 | — |
| Backend API | http://localhost:4111 | — |

Если страница не открывается — в PowerShell заново:

```powershell
kubectl port-forward -n logging svc/kibana-kibana 5601:5601
kubectl port-forward -n monitoring svc/monitoring-grafana 3001:80
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
kubectl port-forward svc/backend 4111:4111
```

> На Windows + Minikube (Docker) адрес `192.168.49.2:30601` обычно **не работает**. Нужен именно `kubectl port-forward` → `localhost`.

---

## Kibana (логи)

1. Открой http://localhost:5601  
2. Слева **Discover** (или Analytics → Discover)  
3. Data View уже создан: **`nbank-logs-*`** (time field `@timestamp`)  
4. Справа сверху поставь время **Last 24 hours** / **Last 1 hour**

### Поиски для скриншотов

| Что | KQL / Lucene в строке поиска |
|-----|------------------------------|
| Успешные переводы | `message: "Transfer successful"` |
| Неуспешные переводы | `message: "Transfer failed"` |
| Запросы на перевод | `message: "Transfer request"` |
| Логины | `message: login` или `message: "User logged"` |
| Создание пользователя | `message: "User created"` OR `message: create*` |

Всплески ошибок: график гистограммы сверху в Discover при фильтре `Transfer failed`.

---

## Grafana (метрики)

1. http://localhost:3001 → `admin` / `admin`  
2. Dashboards → **NBank Business Metrics**  
   (или прямо: http://localhost:3001/d/nbank-business/nbank-business-metrics)  
3. Скриншоты: успешные/неуспешные транзакции, создание пользователей, логины

---

## Генератор активности

Уже запущен на ~25 минут (`infra/monitoring/activity-run.log`).

Повторить вручную:

```powershell
cd infra\monitoring
kubectl port-forward svc/backend 4111:4111   # в отдельном окне
.\generate-activity.ps1 -BaseUrl "http://127.0.0.1:4111" -DurationMinutes 30 -IntervalSeconds 3
```

---

## Часть 5 — как получить ответы

В Prometheus → Graph вставь запрос и нажми Execute.  
Или смотри актуальные значения на backend: http://localhost:4111/actuator/prometheus

### PromQL (за сутки)

```promql
sum(increase(user_created_total[24h]))
sum(increase(user_login_total[24h]))
sum(increase(customer_profile_fetched_total[24h]))
sum(increase(customer_profile_updated_total[24h]))
sum(increase(account_created_total[24h]))
sum(increase(admin_user_listed_total[24h]))
sum(increase(admin_user_deleted_total[24h]))
sum(increase(transfer_started_total[24h]))
sum(increase(transfer_success_total[24h]))
sum(increase(transfer_failed_total[24h]))
sum(increase(account_transactions_viewed_total[24h]))
```

Проценты:

```promql
sum(increase(transfer_success_total[24h])) / sum(increase(transfer_started_total[24h])) * 100
sum(increase(transfer_failed_total[24h])) / sum(increase(transfer_started_total[24h])) * 100
```

Всплески ошибок по времени (Grafana / Prometheus Graph, range):

```promql
sum(rate(transfer_failed_total[1m]))
```

### Вопросы, где нужны логи (Kibana)

| Вопрос | Как смотреть |
|--------|----------------|
| Самый активный по логинам | Discover → `message: *login*` → Aggregate / вручную по `user=` в message |
| Кто чаще обновлял профиль | `message: *profile*` / `updated` |
| Кто чаще ошибался на переводах | `message: "Transfer failed"` → смотри `user='...'` в тексте |
| Средняя сумма перевода | `message: "Transfer request"` → amount=... |
| Причины ошибок | уникальные тексты `Transfer failed: ...` |
| Создание user >3 раза подряд | фильтр failed create + timeline |
| Метрики vs логи | сравни `transfer_failed_total` с числом `Transfer failed` в Discover |

---

## Снимок метрик (во время генератора, пример)

Снимай **после** окончания генератора ещё раз — цифры вырастут.

| Метрика | Пример значения (на момент запуска) |
|---------|--------------------------------------|
| user_created_total | 23+ |
| user_login_total | 44+ |
| customer_profile_fetched_total | 21+ |
| customer_profile_updated_total | 21+ |
| account_created_total | 23+ |
| admin_user_listed_total | 24+ |
| admin_user_deleted_total | растёт каждые 7 тиков |
| transfer_started_total | 116+ |
| transfer_success_total | 44+ |
| transfer_failed_total | 72+ |
| account_transactions_viewed_total | 21+ |

Логи неуспешных переводов уже есть, например:

```text
Transfer failed: insufficient funds or invalid accounts, from=6, to=999999
```

---

## Чеклист сдачи

- [ ] Скрин Grafana: успешные/неуспешные транзакции  
- [ ] Скрин Grafana: создание пользователей  
- [ ] Скрин Grafana: логины  
- [ ] Скрин Kibana: успешные переводы  
- [ ] Скрин Kibana: неуспешные переводы  
- [ ] Текстовые ответы на вопросы части 5 + скрины, подтверждающие цифры  
- [ ] Файлы `infra/monitoring`, `infra/logging` в ветке `homework/monitoring`  
