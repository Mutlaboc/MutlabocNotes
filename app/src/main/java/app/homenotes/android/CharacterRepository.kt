package app.homenotes.android

interface CharacterDataSource {
    suspend fun getCharacter(): Result<CharacterSheet>
    suspend fun updateCharacter(sheet: CharacterSheet): Result<CharacterSheet>
    suspend fun addExperience(characterXp: Int, skillKey: String?, skillXp: Int): Result<CharacterSheet>
    suspend fun upgradeStat(sheet: CharacterSheet, statKey: String, cost: Int): Result<CharacterSheet> =
        updateCharacter(sheet)
    suspend fun rename(sheet: CharacterSheet): Result<CharacterSheet> = updateCharacter(sheet)
}
