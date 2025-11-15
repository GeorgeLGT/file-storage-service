# Дипломный проект «Облачное хранилище»

REST-сервис для безопасного хранения и управления файлами с веб-интерфейсом.

## Описание проекта

Сервис предоставляет защищенное облачное хранилище файлов с возможностью:

* Авторизации пользователей
* Загрузки файлов
* Просмотра списка файлов
* Скачивания файлов
* Переименования файлов
* Удаления файлов

## Технологии

### Backend

* **Java 17** + **Spring Boot 3**
* **Spring Security** с JWT аутентификацией
* **PostgreSQL** - хранение данных пользователей
* **JPA/Hibernate**
* **JUnit 5** + **Testcontainers** - тестирование

## API Endpoints

### Аутентификация

* `POST /cloud/login` - вход в систему
* `POST /cloud/logout` - выход из системы

### Управление файлами

* `GET /cloud/list` - список файлов пользователя
* `POST /cloud/file` - загрузка файла
* `GET /cloud/file` - скачивание файла
* `PUT /cloud/file` - переименование файла
* `DELETE /cloud/file` - удаление файла

## Начальные пользователи

Система создает тестовых пользователей при первом запуске:


| Email             | Пароль |
| ----------------- | ------------ |
| user@example.com  | password     |
| admin@example.com | admin        |
| test@example.com  | test         |

## Разработка

### Тестирование

```
 Backend тесты
./gradlew test
```

### Сборка

```
 Backend
./gradlew build

 Frontend
npm run build
```

## Запуск приложения

### Предварительные требования

* Docker и Docker Compose
* Java 17 (для разработки)
* Node.js 16+ (для разработки фронтенда)

### Запуск через Docker Compose

1. Клонировать репозиторий
2. Запустить контейнеры:

```
docker-compose up -d
```

Сервисы будут доступны:

* **Frontend**: [http://localhost:8080](http://localhost:8080/)
* **Backend**: [http://localhost:8081](http://localhost:8081/)

### Ручной запуск

#### Backend

```
./gradlew bootRun
```

#### Frontend

```
cd frontend
npm install
npm run serve
