package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.HomeCardsApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface HomeInfoDataSource {
    suspend fun getAllCards(): Result<List<HomeInfoCard>>
    suspend fun insert(card: HomeInfoCard): Result<HomeInfoCard>
    suspend fun update(card: HomeInfoCard): Result<HomeInfoCard>
    suspend fun delete(cardId: String): Result<Unit>
}

// Репозиторий для карточек домашнего экрана.
// API передаётся снаружи, чтобы общий Retrofit создавался в AppContainer.
class HomeInfoRepository(
    private val api: HomeCardsApi,
) : HomeInfoDataSource {

    // Загружает все карточки и отдаёт ошибку наружу, чтобы экран мог показать сообщение.
    override suspend fun getAllCards(): Result<List<HomeInfoCard>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.success(api.getHomeCards().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Creates a card and returns the backend-canonical response, including server timestamps.
    override suspend fun insert(card: HomeInfoCard): Result<HomeInfoCard> = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createHomeCard(card.toUpsertRequestDto())
            Result.success(created.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates a card and returns the backend-canonical response, including server timestamps.
    override suspend fun update(card: HomeInfoCard): Result<HomeInfoCard> = withContext(Dispatchers.IO) {
        if (card.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        }

        return@withContext try {
            val updated = api.updateHomeCard(card.id, card.toUpsertRequestDto())
            Result.success(updated.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Удаляет карточку на backend и возвращает результат операции во ViewModel.
    override suspend fun delete(cardId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (cardId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        }

        return@withContext try {
            api.deleteHomeCard(cardId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
