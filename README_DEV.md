# README для разработки

Документ описывает конфигурацию Android-сборок Mutlaboc Notes после разделения окружений.

## Окружения и сборки

В приложении есть три product flavor:

- `dev` - локальная разработка.
- `stage` - проверка на staging-окружении.
- `prod` - production-сборка.

Основные команды:

```powershell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:assembleStageDebug
.\gradlew.bat :app:assembleProdRelease
```

Для unit-тестов по окружениям:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
.\gradlew.bat :app:testStageDebugUnitTest
.\gradlew.bat :app:testProdDebugUnitTest
```

## Backend URL

Backend URL задается через Gradle properties. Приоритет значений:

1. Параметр командной строки или CI, например `-PPROD_BACKEND_URL=https://example.com/`.
2. `local.properties` в корне проекта.
3. Production fallback, зашитый в Gradle-конфигурации.

Поддерживаемые ключи:

```properties
DEV_BACKEND_URL=http://10.0.2.2:8080/
STAGE_BACKEND_URL=https://stage.example.com/
PROD_BACKEND_URL=https://homenoteapp.ru/
```

Для Android Emulator локальный backend на машине разработчика доступен как:

```properties
DEV_BACKEND_URL=http://10.0.2.2:8080/
```

Если `STAGE_BACKEND_URL` или `PROD_BACKEND_URL` не заданы, сборка использует production fallback. Это сделано намеренно для текущего issue.

## OAuth client IDs

Google и Yandex client IDs передаются через Gradle в `BuildConfig`, resources и manifest placeholders.

Рекомендуемые ключи для окружений:

```properties
DEV_GOOGLE_WEB_CLIENT_ID=...
DEV_YANDEX_CLIENT_ID=...
STAGE_GOOGLE_WEB_CLIENT_ID=...
STAGE_YANDEX_CLIENT_ID=...
PROD_GOOGLE_WEB_CLIENT_ID=...
PROD_YANDEX_CLIENT_ID=...
```

Также поддерживаются общие ключи, если один client ID используется для всех окружений:

```properties
GOOGLE_WEB_CLIENT_ID=...
YANDEX_CLIENT_ID=...
```

OAuth client IDs являются идентификаторами приложения, а не полноценными секретами вроде private key. Тем не менее для разных окружений их лучше задавать через `local.properties`, параметры Gradle или CI secrets, чтобы не смешивать dev/stage/prod настройки.

## Пример local.properties

`local.properties` не коммитится в репозиторий. Помимо `sdk.dir`, в него можно добавить локальную конфигурацию:

```properties
sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk
DEV_BACKEND_URL=http://10.0.2.2:8080/
DEV_GOOGLE_WEB_CLIENT_ID=...
DEV_YANDEX_CLIENT_ID=...
```

Для CI те же значения можно передавать как Gradle properties:

```powershell
.\gradlew.bat :app:assembleProdRelease `
  -PPROD_BACKEND_URL=https://homenoteapp.ru/ `
  -PPROD_GOOGLE_WEB_CLIENT_ID=... `
  -PPROD_YANDEX_CLIENT_ID=...
```

## Release signing

Release signing config не меняется в рамках этой задачи. Если signing config понадобится для production-релиза, храните keystore, passwords и aliases вне репозитория и подключайте их через `local.properties`, CI secrets или отдельную защищенную конфигурацию сборки.
