package app.homenotes.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Полоса событий фокус-таймера. Раз в минуту работы таймера [onMinuteTick] роллит
 * случайное событие через [FocusEventsRepository] (награды применяются локально и
 * уходят на бекенд идемпотентным клеймом) и добавляет запись в [feed].
 */
class FocusEventsViewModel(
    application: Application,
    private val repository: FocusEventsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    /** Записи текущей сессии таймера, новые в конце. */
    var feed by mutableStateOf<List<FocusFeedEntry>>(emptyList())
        private set

    /** Начало новой сессии таймера — полоса очищается. */
    fun startSession() {
        feed = emptyList()
        repository.onSessionStart()
    }

    /** Минутный тик работающего таймера. [sessionSkillKey] — навык сессии. */
    fun onMinuteTick(sessionSkillKey: String?) {
        viewModelScope.launch(ioDispatcher) {
            val entry = runCatching { repository.rollEvent(sessionSkillKey, isEnglish()) }.getOrNull()
            if (entry != null) {
                launch(Dispatchers.Main) { feed = feed + entry }
            }
        }
    }

    private fun isEnglish(): Boolean = Locale.getDefault().language.equals("en", ignoreCase = true)
}
