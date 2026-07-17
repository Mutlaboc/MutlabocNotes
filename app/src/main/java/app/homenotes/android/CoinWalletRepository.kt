package app.homenotes.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Кошелёк потраченных монет. Заработанные монеты считаются на лету из выполненных
 * заметок ([NotesUiState.Content.totalCoins]), поэтому отдельно хранится только сумма
 * списаний на прокачку характеристик. Доступный баланс = заработано − потрачено.
 *
 * Списания привязаны к аккаунту ([userKey]), чтобы у каждого пользователя был свой баланс.
 */
interface CoinWalletRepository {
    /** Сколько монет пользователь уже потратил на прокачку. */
    suspend fun spentCoins(userKey: String): Int

    /** Добавляет [amount] к сумме списаний пользователя и возвращает новое значение. */
    suspend fun recordSpend(userKey: String, amount: Int): Int
}

private val Context.coinWalletDataStore by preferencesDataStore(
    name = DataStoreCoinWalletRepository.DATA_STORE_NAME
)

/** DataStore-хранилище потраченных монет. Отдельный store, чтобы не пересекаться с настройками. */
class DataStoreCoinWalletRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) : CoinWalletRepository {

    constructor(context: Context) : this(context.coinWalletDataStore)

    override suspend fun spentCoins(userKey: String): Int = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[spentKey(userKey)] ?: 0 }
        .first()

    override suspend fun recordSpend(userKey: String, amount: Int): Int {
        if (amount <= 0) return spentCoins(userKey)
        val key = spentKey(userKey)
        val updated = dataStore.edit { preferences ->
            preferences[key] = (preferences[key] ?: 0) + amount
        }
        return updated[key] ?: amount
    }

    private fun spentKey(userKey: String) = intPreferencesKey("spent_coins::$userKey")

    internal companion object {
        const val DATA_STORE_NAME = "coin_wallet"
    }
}

/** Лёгкая реализация без Android-зависимостей: используется как безопасное значение по умолчанию. */
class InMemoryCoinWalletRepository : CoinWalletRepository {

    private val spent = ConcurrentHashMap<String, Int>()

    override suspend fun spentCoins(userKey: String): Int = spent[userKey] ?: 0

    override suspend fun recordSpend(userKey: String, amount: Int): Int {
        if (amount <= 0) return spentCoins(userKey)
        return spent.merge(userKey, amount, Int::plus) ?: amount
    }
}
