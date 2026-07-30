package app.homenotes.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Слой палитры — сырые краски дизайн-системы.
 *
 * Здесь нет ни одного решения о применении: имена намеренно ничего не говорят
 * о том, где цвет используется. Смысл назначается уровнем выше, в [Colors].
 *
 * Всё `internal` — композаблы не должны видеть этот файл. Если в экране
 * появилось имя вида `Clay600`, это ошибка, а не сокращение.
 *
 * Правило именования: семейство + ступень светлоты. Меньше число — светлее.
 * Ступени не равномерны: близкие числа означают близкие оттенки.
 *
 * Одна краска может обслуживать несколько ролей и обе темы — это нормально
 * и означает, что значения совпадают, а не что роли связаны.
 */

// ---------------------------------------------------------------------------
// Sand — тёплые нейтрали.
// Единственная сквозная шкала: поверхности и текст обеих тем, плюс COMMON.
// ---------------------------------------------------------------------------

internal val Sand00 = Color(0xFFFFFFFF)  // surface (светлая), textOnAccent (обе)
internal val Sand50 = Color(0xFFFBF8F4)  // background (светлая)
internal val Sand100 = Color(0xFFF2EDE6) // surfaceVariant (светлая), textPrimary (тёмная)
internal val Sand120 = Color(0xFFEFEBE6) // COMMON фон (светлая)
internal val Sand150 = Color(0xFFEDE7DE) // surfaceSunken (светлая), интенсивность 0 (светлая)
internal val Sand200 = Color(0xFFE5DDD2) // border (светлая)
internal val Sand300 = Color(0xFFCFC4B5) // borderStrong (светлая)
internal val Sand400 = Color(0xFFB3AAA0) // textSecondary (тёмная)
internal val Sand500 = Color(0xFF9A9088) // textMuted (светлая), COMMON акцент (тёмная)
internal val Sand550 = Color(0xFF8B837A) // COMMON акцент (светлая)
internal val Sand600 = Color(0xFF837A72) // textMuted (тёмная)
internal val Sand700 = Color(0xFF6A6058) // textSecondary (светлая)
internal val Sand750 = Color(0xFF4C4740) // borderStrong (тёмная)
internal val Sand800 = Color(0xFF38342F) // border (тёмная)
internal val Sand850 = Color(0xFF2C2926) // surfaceVariant (тёмная), COMMON фон (тёмная), интенсивность 0 (тёмная)
internal val Sand870 = Color(0xFF2A2521) // textPrimary (светлая)
internal val Sand880 = Color(0xFF232120) // surface (тёмная)
internal val Sand900 = Color(0xFF1A1816) // background (тёмная)
internal val Sand950 = Color(0xFF151312) // surfaceSunken (тёмная)

// ---------------------------------------------------------------------------
// Clay — глиняный акцент. Главное действие, активное состояние, «сегодня».
// ---------------------------------------------------------------------------

internal val Clay100 = Color(0xFFF7E4DC) // accentSubtle (светлая)
internal val Clay400 = Color(0xFFE07E5F) // accent (тёмная)
internal val Clay500 = Color(0xFFC2664A) // accentPressed (тёмная)
internal val Clay600 = Color(0xFFC25E42) // accent (светлая)
internal val Clay700 = Color(0xFFA64C33) // accentPressed (светлая)
internal val Clay900 = Color(0xFF3A2721) // accentSubtle (тёмная)

// ---------------------------------------------------------------------------
// Moss — зелёный завершения. Success и шкала интенсивности дня: одна шкала,
// потому что заполненный день и есть визуальный след выполненной работы.
// Не путать с Leaf — тот кодирует редкость.
// ---------------------------------------------------------------------------

internal val Moss100 = Color(0xFFDFF2E8) // successSubtle (светлая)
internal val Moss200 = Color(0xFFC9E6D5) // интенсивность 1 (светлая)
internal val Moss300 = Color(0xFF92D0AE) // интенсивность 2 (светлая)
internal val Moss400 = Color(0xFF5CB98A) // интенсивность 3 (светлая)
internal val Moss450 = Color(0xFF4FBE8A) // success (тёмная), интенсивность 4 (тёмная)
internal val Moss600 = Color(0xFF2E9E6B) // success (светлая), интенсивность 4 (светлая)
internal val Moss700 = Color(0xFF38805C) // интенсивность 3 (тёмная)
internal val Moss800 = Color(0xFF2A5C43) // интенсивность 2 (тёмная)
internal val Moss900 = Color(0xFF1F3A2C) // successSubtle (тёмная), интенсивность 1 (тёмная)

// ---------------------------------------------------------------------------
// Brick — тревога и удаление.
// ---------------------------------------------------------------------------

internal val Brick100 = Color(0xFFFAE3E1) // dangerSubtle (светлая)
internal val Brick400 = Color(0xFFE06A60) // danger (тёмная)
internal val Brick600 = Color(0xFFB23A32) // danger (светлая)
internal val Brick900 = Color(0xFF3A211F) // dangerSubtle (тёмная)

// ---------------------------------------------------------------------------
// Amber — приближающийся дедлайн.
// ---------------------------------------------------------------------------

internal val Amber100 = Color(0xFFFBEEDA) // warningSubtle (светлая)
internal val Amber400 = Color(0xFFE8A845) // warning (тёмная)
internal val Amber600 = Color(0xFFD98A1F) // warning (светлая)
internal val Amber900 = Color(0xFF3A2D18) // warningSubtle (тёмная)

// ---------------------------------------------------------------------------
// Gold — монеты и LEGENDARY. Совпадение намеренное: высшая редкость и валюта
// принадлежат одному контуру награды.
// ---------------------------------------------------------------------------

internal val Gold100 = Color(0xFFFBEFD6) // coinSubtle (светлая), LEGENDARY фон (светлая)
internal val Gold400 = Color(0xFFEFB945) // coin (тёмная), LEGENDARY акцент (тёмная)
internal val Gold500 = Color(0xFFDFA22B) // coin (светлая)
internal val Gold600 = Color(0xFFD99A1E) // LEGENDARY акцент (светлая)
internal val Gold900 = Color(0xFF3A2E17) // coinSubtle (тёмная), LEGENDARY фон (тёмная)

// ---------------------------------------------------------------------------
// Редкость: Leaf / Azure / Iris. COMMON берётся из Sand, LEGENDARY — из Gold.
// ---------------------------------------------------------------------------

internal val Leaf100 = Color(0xFFE2F2E5)  // UNCOMMON фон (светлая)
internal val Leaf400 = Color(0xFF55B872)  // UNCOMMON акцент (тёмная)
internal val Leaf600 = Color(0xFF3E9E5C)  // UNCOMMON акцент (светлая)
internal val Leaf900 = Color(0xFF1F3324)  // UNCOMMON фон (тёмная)

internal val Azure100 = Color(0xFFE1ECF9) // RARE фон (светлая)
internal val Azure400 = Color(0xFF5A96D8) // RARE акцент (тёмная)
internal val Azure600 = Color(0xFF3B7CC4) // RARE акцент (светлая)
internal val Azure900 = Color(0xFF1E2B3A) // RARE фон (тёмная)

internal val Iris100 = Color(0xFFECE5F8)  // EPIC фон (светлая)
internal val Iris400 = Color(0xFF9C74DE)  // EPIC акцент (тёмная)
internal val Iris600 = Color(0xFF8257C9)  // EPIC акцент (светлая)
internal val Iris900 = Color(0xFF2A2138)  // EPIC фон (тёмная)

// ---------------------------------------------------------------------------
// Категории заметок: Mist / Cocoa / Sage. Приглушённые — это контент, а не
// награда. Ступени по одной схеме: 100 — фон светлой, 200 — текст тёмной,
// 700 — текст светлой, 900 — фон тёмной.
// ---------------------------------------------------------------------------

internal val Mist100 = Color(0xFFE3EEF5)  // SHOPPING фон (светлая)
internal val Mist200 = Color(0xFFCFE3EF)  // SHOPPING текст (тёмная)
internal val Mist700 = Color(0xFF2C4A5C)  // SHOPPING текст (светлая)
internal val Mist900 = Color(0xFF1E2E38)  // SHOPPING фон (тёмная)

internal val Cocoa100 = Color(0xFFF6E7E2) // TASKS фон (светлая)
internal val Cocoa200 = Color(0xFFF2DCD4) // TASKS текст (тёмная)
internal val Cocoa700 = Color(0xFF6B3F31) // TASKS текст (светлая)
internal val Cocoa900 = Color(0xFF38251F) // TASKS фон (тёмная)

internal val Sage100 = Color(0xFFE7EFE5)  // RECURRING_TASKS фон (светлая)
internal val Sage200 = Color(0xFFDAE8D6)  // RECURRING_TASKS текст (тёмная)
internal val Sage700 = Color(0xFF3B4F38)  // RECURRING_TASKS текст (светлая)
internal val Sage900 = Color(0xFF22301F)  // RECURRING_TASKS фон (тёмная)