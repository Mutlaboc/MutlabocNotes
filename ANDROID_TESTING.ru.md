# Тестирование Android-релиза

Используйте этот чеклист перед продвижением release candidate. Он разделяет compile
smoke, ручную social-auth проверку и Android 12/13/14 notification regression, чтобы
release gate не зависел от локальных секретов, но реальные OAuth-сценарии не потерялись.

## Локальные проверки перед RC

Запускать из корня Android-репозитория:

```powershell
.\gradlew.bat assembleDevDebug testDevDebugUnitTest lintDevDebug
```

Ожидаемый результат:

- Debug APK собирается успешно.
- Unit tests проходят.
- Новые warnings или failures не принимаются без связанного blocker.
- Реальные Google/Yandex client IDs не требуются: `dev` flavor использует compile-safe
  placeholders, если values не переданы явно.

## Social auth smoke

Перед RC отдельно проверьте Google/Yandex sign-in на `stageDebug` или другом выбранном
окружении с настоящими OAuth client IDs:

- Google sign-in возвращает id token, backend login проходит успешно.
- Yandex sign-in возвращает access token, backend login проходит успешно.
- Отмена, пустой token и ошибка SDK показываются через snackbar/UiMessage.
- Client IDs берутся из `local.properties`, Gradle properties, environment variables
  или CI secrets; placeholders из `dev` flavor для этой проверки не подходят.

## Connected smoke suite

Перед release builds запустить критический Compose UI smoke suite:

```powershell
.\gradlew.bat :app:connectedDevDebugAndroidTest
```

Этот suite покрывает auth, home, settings и logout path на подключенном emulator или
device. Запустите его минимум один раз на каждом целевом API level релиза:

| Android version | API level | Device or emulator | Build | Tester | Result | Blockers |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

## Ручная RC regression matrix

Выполните эти проверки для Android 12, 13 и 14. Любой failure записывайте в
`RELEASE_CHECKLIST.md` с build, device и backend environment.

| Area | Required checks | Android 12 | Android 13 | Android 14 |
| --- | --- | --- | --- | --- |
| Auth | Register или sign in, invalid credentials, social auth availability, `/auth/me` session restore | TBD | TBD | TBD |
| Notes | Create, edit, delete, list refresh, empty state, validation errors | TBD | TBD | TBD |
| Completion | Toggle completion, verify persistence after refresh and app restart | TBD | TBD | TBD |
| Home cards | Create, edit, delete, list refresh, invalid or missing data handling | TBD | TBD | TBD |
| Notifications | One-time deadline, repeating deadline, notification tap opens note, exact-alarm granted/denied fallback, re-entry from Settings | TBD | TBD | TBD |
| Settings | Theme toggle, language switch, persisted preferences after restart | TBD | TBD | TBD |
| Logout | Logout returns to auth, protected screens are inaccessible, refresh token is revoked | TBD | TBD | TBD |
| Backend errors | Airplane/offline mode, backend unavailable, 401 after expired session, retry after recovery | TBD | TBD | TBD |

## Staging backend smoke

Запустите Android app против backend environment, выбранного для RC, и проверьте:

- Public health endpoint успешно отвечает вне приложения.
- Login/register успешно проходит для staging test account.
- Session restore работает после force-close и повторного открытия app.
- Notes CRUD и completion updates проходят успешно.
- Home card CRUD проходит успешно.
- Logout проходит успешно, и тот же refresh token не может восстановить session.

`ApiConfig.BASE_URL` берётся из выбранного Gradle flavor через `BuildConfig`.
Для RC testing предпочитайте `stageDebug` против staging и `prodRelease` для signed
production artifact. Запишите точный backend URL, flavor, version code, version name
и artifact в `RELEASE_CHECKLIST.md`.
