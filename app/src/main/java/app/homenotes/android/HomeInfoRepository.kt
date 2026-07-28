package app.homenotes.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface HomeInfoDataSource {
    fun observeCards(): Flow<List<HomeInfoCard>> = flow { emit(getAllCards().getOrThrow()) }
    suspend fun getAllCards(): Result<List<HomeInfoCard>>
    suspend fun insert(card: HomeInfoCard): Result<HomeInfoCard>
    suspend fun update(card: HomeInfoCard): Result<HomeInfoCard>
    suspend fun delete(cardId: String): Result<Unit>
}
