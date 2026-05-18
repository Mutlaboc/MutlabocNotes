# Архитектура Mutlaboc Notes

Этот документ фиксирует устройство Android-приложения после финальной очистки. Он нужен, чтобы через несколько месяцев можно было быстро понять, где создаются зависимости, как устроен сетевой слой и какие части системы связаны с backend.

## Android-приложение

`MutlabocNotesApplication` создаёт один `AppContainer` на процесс приложения. `MainActivity` получает из контейнера `ViewModelProvider.Factory` и не создаёт репозитории, session store или scheduler напрямую.

`AppContainer` отвечает за:

- `SessionManager` как общий `AuthSessionStore` для репозиториев и authenticated Retrofit.
- `AuthRepository`, который работает с login/register/social auth/refresh/logout и хранит сессию.
- `NotesRepository` и `HomeInfoRepository`, которые используют authenticated Retrofit и backend API.
- `DeadlineNotificationScheduler`, который планирует локальные уведомления по дедлайнам задач.
- `DataStoreSettingsRepository`, который хранит настройки темы и языка.
- `MutlabocNotesViewModelFactory`, который создаёт root ViewModel с готовыми зависимостями.

Репозитории остаются тонким слоем между ViewModel и API: они мапят backend DTO в доменные модели, нормализуют ошибки через `ApiErrorMapper` и не знают о Compose-навигации.

## Auth и сессия

Auth API создаётся отдельно от authenticated Retrofit, потому что login/register/refresh не должны автоматически добавлять Bearer token. Защищённые API создаются через `AuthenticatedApiFactory`: interceptor добавляет access token, а authenticator выполняет refresh через `SessionManager`.

При logout или истечении сессии root navigation переводит приложение в auth flow, очищает notes/home-info state и не оставляет локальные данные другого пользователя на экране.

## UI и локальные функции

Основные entry points остаются стабильными: `HomeScreen`, `CompletedNotesScreen`, `EditNoteScreen`, `HomeInfoScreen`, `NoteItem`, `BottomBar`. Вспомогательные Compose-функции разнесены по файлам того же package, чтобы не менять вызовы из `MainActivity` и тестов.

Форматирование дат дедлайнов централизовано в `formatDeadlineDate`. Если формат даты изменится, обновлять нужно этот helper и связанные тесты/скриншоты, а не отдельные экраны.

Пользовательские статические строки должны идти через `strings.xml` и `values-en/strings.xml`. Inline-текст допустим для preview sample data, test tags, route names, exception messages и динамического пользовательского контента.

## Настройки и уведомления

Settings flow хранит тему и язык через DataStore. `SettingsViewModel` получает repository из `AppContainer`, а UI только показывает состояние и отправляет пользовательские действия.

Deadline notifications работают только для активных задач с дедлайном. Scheduler выбирает exact/inexact alarm в зависимости от разрешений Android, сохраняет repeat metadata и передаёт note id в notification intent, чтобы открыть нужную заметку.

## Backend и окружения

Android repo не содержит backend-код. Сервис находится рядом, в `D:\Projects\notes-backend`, и запускается как отдельное Ktor-приложение. Android обращается к нему через `BuildConfig.BACKEND_BASE_URL`, который задаётся flavor-конфигурацией Gradle.

Flavors:

- `dev`: локальная разработка, default `http://10.0.2.2:8080/` для Android Emulator.
- `stage`: staging, берёт `STAGE_BACKEND_URL` или production fallback.
- `prod`: production, берёт `PROD_BACKEND_URL` или production fallback.

Основные переменные Android: `DEV_BACKEND_URL`, `STAGE_BACKEND_URL`, `PROD_BACKEND_URL`, `*_GOOGLE_WEB_CLIENT_ID`, `*_YANDEX_CLIENT_ID`, release signing keys. Backend-переменные (`DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, OAuth client IDs/secrets) описаны в `notes-backend/README.md` и `notes-backend/RELEASE_RUNBOOK.ru.md`.

## Проверки перед изменениями

Для Android минимум:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

Для backend при изменениях контрактов или deployment-документации:

```powershell
cd D:\Projects\notes-backend
.\gradlew.bat test
```

Если меняются backend DTO или error contract, нужно синхронно проверить Android contract fixtures в `app/src/test/resources/contracts` и backend fixtures в `src/test/resources/contracts`.
