# Тестирование Android-релиза

Используйте этот чеклист перед продвижением release candidate. Он намеренно остается
ручным: в рамках этого шага release hardening не меняются runtime-код, Gradle wiring,
CI jobs или переключение backend URL.

## Локальные проверки перед RC

Запускать из корня Android-репозитория:

```powershell
.\gradlew.bat assembleDevDebug testDevDebugUnitTest lintDevDebug
```

Ожидаемый результат:

- Debug APK собирается успешно.
- Unit tests проходят.
- Новые warnings или failures не принимаются без связанного blocker.

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

Текущий checked-in `ApiConfig.BASE_URL` указывает на `https://homenoteapp.ru/`.
Эта issue не меняет этот URL, не добавляет product flavors и не вводит automatic staging
switching. Если RC тестируется против staging, используйте утвержденный командой local
build или environment override process и запишите точный backend URL в
`RELEASE_CHECKLIST.md`.
