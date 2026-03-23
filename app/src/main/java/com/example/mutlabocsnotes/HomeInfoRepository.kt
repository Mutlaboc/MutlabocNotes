package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.HomeCardsApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class HomeInfoRepository(
    baseUrl: String = ApiConfig.BASE_URL,
    private val firebaseUidProvider: FirebaseUidProvider = FirebaseUidProvider(),
    private val api: HomeCardsApi = createHomeCardsApi(baseUrl),
) {

    suspend fun getAllCards(): Result<List<HomeInfoCard>> = withContext(Dispatchers.IO) {
        val uid = firebaseUidProvider.getUidOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        return@withContext try {
            Result.success(api.getHomeCards(uid).map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insert(card: HomeInfoCard): Result<String> = withContext(Dispatchers.IO) {
        val uid = firebaseUidProvider.getUidOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        return@withContext try {
            val created = api.createHomeCard(uid, card.toUpsertRequestDto())
            Result.success(created.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun update(card: HomeInfoCard): Result<Unit> = withContext(Dispatchers.IO) {
        if (card.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        }

        val uid = firebaseUidProvider.getUidOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        return@withContext try {
            api.updateHomeCard(uid, card.id, card.toUpsertRequestDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(cardId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (cardId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        }

        val uid = firebaseUidProvider.getUidOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        return@withContext try {
            api.deleteHomeCard(uid, cardId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private fun createHomeCardsApi(baseUrl: String): HomeCardsApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HomeCardsApi::class.java)
        }
    }
}
