![build](https://github.com/DireElf/SEO_Analyzer/actions/workflows/build.yml/badge.svg)


### Небольшой учебный проект (веб-приложение) на Javalin, запускается с помощью make build-run
- после запуска располагается на http://127.0.0.1:5000
- при запуске с переменной окружения APP_ENV=development используется встроенная БД H2
- при запуске с переменной окружения APP_ENV=production используется БД PostgreSQL и требуется указать переменные PGUSER, PGPASSWORD, PGHOST, PGPORT и PGDATABASE

### Сборка докер-образа
`docker build -t seo-analyzer .`

### Запуск девелопмент-контейнера
`docker run -e APP_ENV=development -p 5000:5000 seo-analyzer`
(для APP_ENV-production дополнительно передать переменные с данными соединения к БД)
