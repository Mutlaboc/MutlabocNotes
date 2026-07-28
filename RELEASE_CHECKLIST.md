# Release Checklist — HomeNotes / Mutlaboc Notes (RuStore)

Чек-лист для подготовки и публикации релиза в RuStore. Заполняется на каждый
production-релиз. Источник истины по версии — `app/build.gradle.kts`.

## Текущий релиз

| Поле | Значение |
| --- | --- |
| versionName | 1.0.1 |
| versionCode | 2 |
| applicationId | app.homenotes.android |
| Flavor для публикации | `prod` (`assembleProdRelease` / `bundleProdRelease`) |
| Git SHA | _заполнить перед сборкой_ |
| Дата сборки | _заполнить_ |
| Go / No-go | _заполнить_ |

## 1. Код и ветка

- [ ] Все нужные изменения влиты в `develop`, рабочая ветка чистая (`git status` пуст).
- [ ] Line-ending churn устранён: после добавления `.gitattributes` выполнено
      `git add --renormalize . && git commit` (либо `git checkout -- .` если изменения только CRLF).
- [ ] Создан релизный тег `android-v1.0.1` от выверенного коммита.

## 2. Конфигурация сборки

- [ ] `targetSdk` / `compileSdk` = 34, `minSdk` = 24.
- [ ] `versionCode` увеличен относительно прошлого загруженного артефакта (монотонно).
- [ ] `prod` flavor: `usesCleartextTraffic` = false, бэкенд по HTTPS (`https://homenoteapp.ru/`).
- [ ] `PROD_BACKEND_URL`, `PROD_GOOGLE_WEB_CLIENT_ID`, `PROD_YANDEX_CLIENT_ID` заданы
      в `local.properties` / env / CI secrets (не закоммичены).

## 3. Подпись

- [ ] Keystore существует и доступен по пути `ANDROID_KEYSTORE_FILE`
      (сейчас `D:\Projects\secure\homenotes-release.jks`).
- [ ] **Сделана резервная копия keystore и паролей** в надёжном месте (потеря = невозможность обновлений).
- [ ] `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` заданы вне репозитория.
- [ ] Собранный артефакт подписан release-ключом (проверить `apksigner verify` / `bundletool`).

## 4. Сборка артефакта

- [ ] `./gradlew clean :app:bundleProdRelease` (AAB) — основной формат для RuStore.
- [ ] При необходимости `:app:assembleProdRelease` (APK).
- [ ] (Опционально, hardening) Включить R8: `isMinifyEnabled = true`, пересобрать,
      прогнать smoke-тест с правилами из `app/proguard-rules.pro`.

## 5. Smoke-тест на реальном устройстве (prod-сборка)

- [ ] Регистрация и вход по email/паролю.
- [ ] Вход через Яндекс.
- [ ] Вход через Google (учесть: не работает на устройствах без Google Mobile Services —
      убедиться, что email/Яндекс полностью покрывают сценарий входа).
- [ ] Заметки: создание, редактирование, удаление, синхронизация.
- [ ] Чек-листы и отметка выполнения.
- [ ] Домашние карточки CRUD.
- [ ] Дедлайны и локальные уведомления (включая точные напоминания).
- [ ] Тема и язык.
- [ ] Проверка на устройстве без GMS (если доступно).

## 6. Юридические документы (требование RuStore)

- [ ] Политика конфиденциальности опубликована публично и доступна по URL.
- [ ] Пользовательское соглашение опубликовано.
- [ ] Инструкция удаления аккаунта опубликована.
- [ ] Ссылки на документы добавлены на лендинг `homenoteapp.ru` (футер).
- [ ] URL политики конфиденциальности указан в карточке приложения RuStore.
- [ ] URL инструкции удаления аккаунта указан в карточке RuStore.
- [ ] Поля в `legal/SUPPORT_CONTACT.ru.md` (раздел 8) заполнены реальными URL.

Рекомендуемые URL после публикации:

- Политика: `https://homenoteapp.ru/legal/privacy.html`
- Соглашение: `https://homenoteapp.ru/legal/terms.html`
- Удаление аккаунта: `https://homenoteapp.ru/legal/account-deletion.html`
- Собираемые данные: `https://homenoteapp.ru/legal/data-collected.html`
- Поддержка: `https://homenoteapp.ru/legal/support.html`

## 7. Карточка приложения RuStore

- [ ] Название, краткое и полное описание.
- [ ] Иконка 512×512.
- [ ] Скриншоты (телефон; при поддержке — планшет).
- [ ] Категория и возрастной рейтинг.
- [ ] Email поддержки: `homenotessupp@yandex.ru`.
- [ ] Декларация разрешений: обоснование `SCHEDULE_EXACT_ALARM` (точные напоминания о дедлайнах),
      `POST_NOTIFICATIONS`, `INTERNET`.
- [ ] Декларация собираемых данных по `legal/DATA_COLLECTED.ru.md` (раздел 10 — краткая формулировка).

## 8. Бэкенд

- [ ] Prod-бэкенд `https://homenoteapp.ru/` доступен и совместим с релизной сборкой.
- [ ] Локализация ПД в РФ подтверждена (хостинг `firstvds.ru`).

## 9. После публикации

- [ ] Релиз-заметки внесены в `CHANGELOG.md`.
- [ ] Тег запушен: `git push origin android-v1.0.1`.
- [ ] Зафиксированы: Git SHA, артефакт, решение go/no-go (в таблице выше).
- [ ] Crash intake: без telemetry SDK — отслеживать обращения на `homenotessupp@yandex.ru`
      и отчёты RuStore Console.
