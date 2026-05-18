# README для разработки

Документ описывает локальную разработку Android-приложения Mutlaboc Notes, его окружения, связь с backend, тесты и release build.

## Ветки и рабочий процесс

Основная рабочая ветка Android-приложения - `develop`. Перед началом задачи проверьте состояние:

```powershell
git status --short --branch
```

Не коммитьте локальные артефакты IDE, Gradle, Kotlin compiler session files, JVM crash logs и replay logs. Они игнорируются через `.gitignore`.

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

В Android Studio для локальной разработки выбирайте variant `devDebug`.

## Backend URL

Backend URL задаётся через Gradle properties. Приоритет значений:

1. Параметр командной строки или CI, например `-PPROD_BACKEND_URL=https://example.com/`.
2. `local.properties` в корне проекта.
3. Production fallback, заданный в Gradle-конфигурации.

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

Если `STAGE_BACKEND_URL` или `PROD_BACKEND_URL` не заданы, сборка использует production fallback. Это ожидаемое поведение текущей конфигурации.

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

## Запуск backend

Backend находится в соседнем репозитории `D:\Projects\notes-backend`. Android-репозиторий не дублирует backend deployment runbook, а ссылается на документы backend-проекта:

- `D:\Projects\notes-backend\README.md` - локальный build/run.
- `D:\Projects\notes-backend\RELEASE_RUNBOOK.ru.md` - rollout, migrations, staging smoke и rollback.

Минимальная локальная проверка backend:

```powershell
cd D:\Projects\notes-backend
.\gradlew.bat test
.\gradlew.bat run
```

Backend ожидает переменные `DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, а также OAuth-настройки Google/Yandex для social auth.

## Тесты

Базовый набор Android unit-тестов:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

Перед release-кандидатом дополнительно проверьте:

- `.\gradlew.bat :app:testStageDebugUnitTest`
- `.\gradlew.bat :app:testProdDebugUnitTest`
- `.\gradlew.bat :app:assembleProdRelease`
- emulator smoke для auth, notes, completed notes, home info, settings и notification navigation.

## Release signing

Release signing config подключается только когда заданы все обязательные значения. Keystore, passwords и alias не коммитятся в репозиторий.

Локальная production-сборка может использовать `local.properties` или Gradle properties:

```properties
ANDROID_KEYSTORE_FILE=C\:\\secure\\mutlaboc-notes-release.jks
ANDROID_KEYSTORE_PASSWORD=...
ANDROID_KEY_ALIAS=...
ANDROID_KEY_PASSWORD=...
```

```powershell
.\gradlew.bat :app:bundleProdRelease :app:assembleProdRelease `
  -PANDROID_KEYSTORE_FILE=C:\secure\mutlaboc-notes-release.jks `
  -PANDROID_KEYSTORE_PASSWORD=... `
  -PANDROID_KEY_ALIAS=... `
  -PANDROID_KEY_PASSWORD=... `
  -PPROD_BACKEND_URL=https://homenoteapp.ru/ `
  -PPROD_GOOGLE_WEB_CLIENT_ID=... `
  -PPROD_YANDEX_CLIENT_ID=...
```

Если signing values не заданы или keystore file недоступен, `release` build type остаётся без `signingConfig`. Это позволяет запускать local validation вроде `:app:assembleProdRelease` без секретов.

GitHub Actions release workflow ожидает secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `PROD_BACKEND_URL`
- `PROD_GOOGLE_WEB_CLIENT_ID`
- `PROD_YANDEX_CLIENT_ID`

## Архитектура и сопровождение

Подробная схема Android-зависимостей, auth/session refresh, repositories, ViewModel factory, settings, notifications и связи с backend описана в `ARCHITECTURE.ru.md`.
