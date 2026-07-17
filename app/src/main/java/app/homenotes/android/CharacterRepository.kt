package app.homenotes.android

import app.homenotes.android.network.CharacterApi
import app.homenotes.android.network.CharacterXpRequestDto
import app.homenotes.android.network.toDomain
import app.homenotes.android.network.toUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CharacterDataSource {
    suspend fun getCharacter(): Result<CharacterSheet>
    suspend fun updateCharacter(sheet: CharacterSheet): Result<CharacterSheet>
    suspend fun addExperience(characterXp: Int, skillKey: String?, skillXp: Int): Result<CharacterSheet>
    suspend fun upgradeStat(sheet: CharacterSheet, statKey: String, cost: Int): Result<CharacterSheet> =
        updateCharacter(sheet)
    suspend fun rename(sheet: CharacterSheet): Result<CharacterSheet> = updateCharacter(sheet)
}

// Репозиторий листа персонажа. API передаётся снаружи (общий Retrofit из AppContainer).
class CharacterRepository(
    private val api: CharacterApi,
) : CharacterDataSource {

    override suspend fun getCharacter(): Result<CharacterSheet> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.success(api.getCharacter().toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCharacter(sheet: CharacterSheet): Result<CharacterSheet> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Result.success(api.updateCharacter(sheet.toUpdateRequest()).toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun addExperience(
        characterXp: Int,
        skillKey: String?,
        skillXp: Int
    ): Result<CharacterSheet> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updated = api.addExperience(
                CharacterXpRequestDto(
                    characterXp = characterXp,
                    skillKey = skillKey,
                    skillXp = skillXp
                )
            )
            Result.success(updated.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
