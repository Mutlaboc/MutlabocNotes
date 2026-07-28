package app.homenotes.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Один шаг обучения с подсказкой над иконкой нижней панели. */
enum class OnboardingHintStep(val id: String) {
    ADD_NOTE("add_note"),
    HOME_INFO("home_info"),
    COMPLETED("completed")
}

/**
 * Прогресс первичного обучения. Каждый флаг означает, что соответствующий
 * экран приветствия или одноразовая подсказка уже были показаны пользователю.
 */
data class OnboardingState(
    val welcomeSeen: Boolean = false,
    val addNoteHintSeen: Boolean = false,
    val homeInfoHintSeen: Boolean = false,
    val completedHintSeen: Boolean = false,
    val noteFormHintSeen: Boolean = false
) {
    fun hintSeen(step: OnboardingHintStep): Boolean = when (step) {
        OnboardingHintStep.ADD_NOTE -> addNoteHintSeen
        OnboardingHintStep.HOME_INFO -> homeInfoHintSeen
        OnboardingHintStep.COMPLETED -> completedHintSeen
    }

    fun withHintSeen(step: OnboardingHintStep): OnboardingState = when (step) {
        OnboardingHintStep.ADD_NOTE -> copy(addNoteHintSeen = true)
        OnboardingHintStep.HOME_INFO -> copy(homeInfoHintSeen = true)
        OnboardingHintStep.COMPLETED -> copy(completedHintSeen = true)
    }

    companion object {
        /** Состояние, в котором всё обучение уже пройдено (для превью и тестов). */
        val Completed = OnboardingState(
            welcomeSeen = true,
            addNoteHintSeen = true,
            homeInfoHintSeen = true,
            completedHintSeen = true,
            noteFormHintSeen = true
        )
    }
}

/**
 * Прогресс обучения хранится отдельно для каждого аккаунта ([userKey]), поэтому
 * каждый новый пользователь видит приветствие и подсказки заново.
 */
interface OnboardingRepository {
    fun state(userKey: String): Flow<OnboardingState>
    suspend fun markWelcomeSeen(userKey: String)
    suspend fun markHintSeen(userKey: String, step: OnboardingHintStep)
    suspend fun markNoteFormHintSeen(userKey: String)
}

private val Context.onboardingDataStore by preferencesDataStore(
    name = DataStoreOnboardingRepository.DATA_STORE_NAME
)

/** DataStore-хранилище прогресса обучения. Отдельный store, чтобы не пересекаться с настройками. */
class DataStoreOnboardingRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    constructor(context: Context) : this(context.onboardingDataStore)

    override fun state(userKey: String): Flow<OnboardingState> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            OnboardingState(
                welcomeSeen = preferences[welcomeKey(userKey)] ?: false,
                addNoteHintSeen = preferences[hintKey(userKey, OnboardingHintStep.ADD_NOTE)] ?: false,
                homeInfoHintSeen = preferences[hintKey(userKey, OnboardingHintStep.HOME_INFO)] ?: false,
                completedHintSeen = preferences[hintKey(userKey, OnboardingHintStep.COMPLETED)] ?: false,
                noteFormHintSeen = preferences[noteFormHintKey(userKey)] ?: false
            )
        }

    override suspend fun markWelcomeSeen(userKey: String) {
        dataStore.edit { preferences ->
            preferences[welcomeKey(userKey)] = true
        }
    }

    override suspend fun markHintSeen(userKey: String, step: OnboardingHintStep) {
        dataStore.edit { preferences ->
            preferences[hintKey(userKey, step)] = true
        }
    }

    override suspend fun markNoteFormHintSeen(userKey: String) {
        dataStore.edit { preferences ->
            preferences[noteFormHintKey(userKey)] = true
        }
    }

    private fun noteFormHintKey(userKey: String) = booleanPreferencesKey("note_form_hint_seen::$userKey")

    private fun welcomeKey(userKey: String) = booleanPreferencesKey("welcome_seen::$userKey")

    private fun hintKey(userKey: String, step: OnboardingHintStep) =
        booleanPreferencesKey("${step.id}_hint_seen::$userKey")

    internal companion object {
        const val DATA_STORE_NAME = "onboarding"
    }
}

/** Лёгкая реализация без Android-зависимостей: используется как безопасное значение по умолчанию. */
class InMemoryOnboardingRepository : OnboardingRepository {

    private val states = MutableStateFlow<Map<String, OnboardingState>>(emptyMap())

    override fun state(userKey: String): Flow<OnboardingState> =
        states.asStateFlow().map { it[userKey] ?: OnboardingState() }

    override suspend fun markWelcomeSeen(userKey: String) {
        update(userKey) { it.copy(welcomeSeen = true) }
    }

    override suspend fun markHintSeen(userKey: String, step: OnboardingHintStep) {
        update(userKey) { it.withHintSeen(step) }
    }

    override suspend fun markNoteFormHintSeen(userKey: String) {
        update(userKey) { it.copy(noteFormHintSeen = true) }
    }

    private fun update(userKey: String, transform: (OnboardingState) -> OnboardingState) {
        states.update { current ->
            val existing = current[userKey] ?: OnboardingState()
            current + (userKey to transform(existing))
        }
    }
}
