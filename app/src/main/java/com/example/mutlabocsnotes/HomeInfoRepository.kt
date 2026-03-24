package com.example.mutlabocsnotes

import android.app.Application
import com.example.mutlabocsnotes.network.HomeCardsApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeInfoRepository(
    application: Application,
    baseUrl: String = ApiConfig.BASE_URL,
    private val api: HomeCardsApi = createHomeCardsApi(application, baseUrl),
) {

    suspend fun getAllCards(): Result<List<HomeInfoCard>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.success(api.getHomeCards().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insert(card: HomeInfoCard): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createHomeCard(card.toUpsertRequestDto())
            Result.success(created.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun update(card: HomeInfoCard): Result<Unit> = withContext(Dispatchers.IO) {
        if (card.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        }

        return@withContext try {
            api.updateHomeCard(card.id, card.toUpsertRequestDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(cardId: String): Result<Unit> = withContext(Dispatchers.IO) {
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

    companion object {
        private fun createHomeCardsApi(
            application: Application,
            baseUrl: String
        ): HomeCardsApi {
            return AuthenticatedApiFactory
                .createRetrofit(application, baseUrl)
                .create(HomeCardsApi::class.java)
        }
    }
}
