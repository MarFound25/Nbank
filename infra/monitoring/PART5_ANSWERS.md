# Часть 5 — ответы (снимок ~17.07.2026, после генератора активности)

Источник: Prometheus (`sum(metric)`), Grafana dashboard **NBank Business Metrics**, Kibana Discover `nbank-logs-*`.

> Цифры — за период работы генератора (условно «сутки» в учебном прогоне; `increase(...[24h])` совпадает с абсолютными counter’ами, т.к. стек поднят в этот день).

---

## Ответы

### Сколько новых пользователей было создано за последние сутки?
**159**  
PromQL: `sum(increase(user_created_total[24h]))`  
Логи: `message: "created successfully with role"` → 159

### Сколько раз пользователи входили в систему?
**316** (`user_login_total`)  
Логи: `message: "Login successful for user"` → 316

### Сколько раз запрашивали профиль клиента, и сколько раз он был обновлён?
- Запросы профиля: **157** (`customer_profile_fetched_total`)
- Обновления профиля: **157** (`customer_profile_updated_total`)  
  В логах попытки: `message: "attempting to update profile"` → 157  
  (часто приходит `Invalid name format` — имя вида `Name 1234` не проходит валидацию)

### Сколько аккаунтов было создано?
**159** (`account_created_total`)  
Логи: `message: "Account created for user"` → 159

### Сколько раз администратор просматривал список пользователей?
**178** (`admin_user_listed_total`)  
Логи: `message: "Admin requested list of all users"` → 178

### Сколько пользователей было удалено админом?
**0** (`admin_user_deleted_total`)  
В логах удалений за прогон не зафиксировано (операция delete в генераторе не отразилась в метрике/логах).

### Сколько переводов было начато?
**873** (`transfer_started_total`)  
Логи: `message: "Transfer request"` → 714 (часть событий есть только в метриках / другом тексте лога)

### Сколько из них были успешными, а сколько — неуспешными?
- Успешные: **315** (`transfer_success_total`)
- Неуспешные: **558** (`transfer_failed_total`)

В логах:
- `Transfer successful` → ~156
- `Transfer failed` → ~403
- `Transfer rejected due to limit` → ~155  
  (403 + 155 ≈ 558 — совпадает с метрикой failed)

### Какой процент успешных / неуспешных переводов?
- Успешные: **315 / 873 ≈ 36.1%**
- Неуспешные: **558 / 873 ≈ 63.9%**

```promql
sum(increase(transfer_success_total[24h])) / sum(increase(transfer_started_total[24h])) * 100
sum(increase(transfer_failed_total[24h])) / sum(increase(transfer_started_total[24h])) * 100
```

### Были ли всплески ошибок при переводах? В какое время?
**Да.** По `sum(rate(transfer_failed_total[1m]))` пики около:
- **16:30**, **16:35–16:36**, **16:38–16:40** (локальное время)  
Причина всплесков: генератор каждые 5 тиков шлёт пачку переводов на несуществующий счёт `to=999999`, плюс суммы `999999` / `-50`.

### Кто был самым активным пользователем по количеству логинов?
**`u0a7d5a`** — **37** успешных логинов  
Kibana: `message: "Login successful for user"` → смотреть `user '...'`

### Кто чаще всего обновлял профиль?
**`u0a7d5a`** — **36** попыток обновления  
`message: "attempting to update profile"`

### Сколько пользователей просматривали список транзакций?
**5 уникальных пользователей**, **153** запроса  
`message: "Request to get transactions"`  
(генератор логинит в основном «топ» ранних пользователей)

### Как изменилась активность по сравнению с предыдущими днями?
Предыдущих дней в этом кластере **нет** (мониторинг/логи подняты сегодня).  
Активность появилась скачком с ~**16:26** (старт генератора) — до этого метрики были ~0.  
Ответ для сдачи: *сравнить не с чем / baseline = 0 до запуска генератора; после 16:26 резкий рост*.

### Каков процент успешных транзакций за сутки?
**≈ 36.1%** (см. выше)

### Кто был самым активным пользователем? Какая средняя сумма перевода за сутки? Какие причины ошибок встречаются чаще всего?
- Самый активный (логины + профиль + транзакции): **`u0a7d5a`**
- Средняя сумма **успешного** перевода (из логов `Transfer successful`, amount=): **≈ 85.91**
- Частые причины ошибок:
  1. **`insufficient funds or invalid accounts`** (`Transfer failed`) — в т.ч. `to=999999`
  2. **`Transfer rejected due to limit`** — amount `999999`
  3. Негативные суммы `amount=-50` → failed / invalid accounts

### Какие пользователи чаще всего сталкивались с ошибками при переводах?
Топ по «плохим» transfer request (limit / invalid account / negative):
1. **`ufaa9b5`** — ~250 (на него приходились error-burst на `to=999999`)
2. `ub6aa25`, `uf49750`, `u0a7d5a` — по ~14

### Были ли случаи, когда создание пользователя падало более 3 раз подряд?
Генератор **каждый тик** шлёт **3** заведомо невалидных create (`username=ab`, weak password).  
В application-логах успешные create есть, а отдельные ERROR по `ab` почти не пишутся (валидация на API без business-log).  
По скрипту/тикам: **да, регулярно по 3 подряд failure** (см. `activity-run.log`: `expected user create failure #1..#3`).  
В метриках failures create отдельного counter нет — только рост `user_created_total` по успешным.

### Сопоставьте метрики и логи: есть ли рост метрик, который не отражён в логах?
**Частично да, по переводам:**

| Метрика | Metrics | Логи (phrase) | Комментарий |
|---------|---------|---------------|-------------|
| user_created / login / accounts / profile / admin list | совпадают | совпадают | OK |
| transfer_failed | 558 | failed 403 + rejected 155 | нужно смотреть **два** текста лога |
| transfer_success | 315 | Transfer successful ~156 | **метрика выше логов** ≈ в 2 раза |
| transfer_started | 873 | Transfer request ~714 | метрика выше |

Вывод для сдачи: рост `transfer_success_total` **не полностью** отражён фразой `Transfer successful` в Kibana (возможны другой текст лога / не все реплики backend в Filebeat / задержка индекса). Для failed метрика сходится, если учесть и `Transfer rejected due to limit`.

---

## Скриншоты к ответам (что приложить)

1. Grafana: Transfers + User operations (уже есть)
2. Kibana: `Transfer successful` / `Transfer failed` (уже есть)
3. Prometheus Graph: `sum(rate(transfer_failed_total[1m]))` за последний час — всплески
4. Kibana: `Login successful for user 'u0a7d5a'` — топ логинов
5. Kibana: `Transfer rejected due to limit` — вторая причина ошибок

---

## Как переснять цифры

```powershell
# Prometheus
http://localhost:9090
# query example
sum(user_created_total)

# Backend
http://localhost:4111/actuator/prometheus

# Kibana
http://localhost:5601/app/discover
```
