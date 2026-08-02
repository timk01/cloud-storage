<h1 align="center"> Cloud-storage </h1>

<p align="center">
  <img src="docs/screenshots/welcome.png" alt="welcome" width="320">
</p>

Многопользовательское файловое облако. Пользователи сервиса могут использовать его для загрузки и хранения файлов.

```dotenv
POSTGRES_USER=cloud_user
POSTGRES_PASSWORD=cloud_password
POSTGRES_DB=cloud_storage_db

MINIO_USER=minio_admin
MINIO_PASSWORD=minio_password
```

Файл `.env` не должен попадать в Git:

```gitignore
.env
```

Запустите инфраструктуру:

```bash
docker compose up -d
```

## 2. Настройка Spring Boot

В `src/main/resources/application.yaml` используются переменные окружения:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}

minio:
  url: ${MINIO_URL:http://localhost:9000}
  user: ${MINIO_USER}
  password: ${MINIO_PASSWORD}
```

В IntelliJ IDEA откройте:

```text
Run → Edit Configurations → CloudStorageApplication
```

В поле `Environment variables` добавьте:

```text
POSTGRES_DB=cloud_storage_db;POSTGRES_USER=cloud_user;POSTGRES_PASSWORD=cloud_password;MINIO_USER=minio_admin;MINIO_PASSWORD=minio_password
```

Значения должны совпадать со значениями в `.env`.

После этого запустите `CloudStorageApplication`.

---

## Контакты

Автор: [@timk01](https://github.com/timk01)  
Телеграмм: https://t.me/tim_matv
