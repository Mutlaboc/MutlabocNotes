package app.homenotes.android

import android.app.Application
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun sheetWith(strength: Int): CharacterSheet = CharacterSheet(
        name = "Hero",
        level = 1,
        xp = 0,
        xpToNext = 100,
        portraitRes = 0,
        stats = listOf(
            CharacterStat("Сила", "", strength, key = "str"),
            CharacterStat("Ловкость", "", 1, key = "dex")
        ),
        skills = emptyList()
    )

    @Test
    fun statUpgradeCost_growsQuadraticallyWithValue() {
        assertEquals(1, statUpgradeCost(1))
        assertEquals(25, statUpgradeCost(5))
        assertEquals(100, statUpgradeCost(10))
    }

    @Test
    fun upgradeStat_deductsCoins_incrementsStat_andPersists() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeCharacterDataSource(sheetWith(strength = 5))
        val wallet = InMemoryCoinWalletRepository()
        val viewModel = CharacterViewModel(Application(), repo, wallet, mainDispatcherRule.dispatcher)

        viewModel.loadCharacter()
        viewModel.setUser("user@example.com")
        advanceUntilIdle()

        // Cost for value 5 is 25; we have 50 earned coins, so the upgrade goes through.
        viewModel.upgradeStat("str", earnedCoins = 50)
        advanceUntilIdle()

        val sheet = (viewModel.uiState as CharacterUiState.Content).sheet
        assertEquals(6, sheet.stats.first { it.key == "str" }.value)
        assertEquals(25, viewModel.spentCoins)
        assertEquals(25, wallet.spentCoins("user@example.com"))
        assertEquals(1, repo.updateCount)
    }

    @Test
    fun upgradeStat_doesNothing_whenCoinsInsufficient() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeCharacterDataSource(sheetWith(strength = 5))
        val viewModel = CharacterViewModel(
            Application(), repo, InMemoryCoinWalletRepository(), mainDispatcherRule.dispatcher
        )
        viewModel.loadCharacter()
        viewModel.setUser("user@example.com")
        advanceUntilIdle()

        // Cost 25 > available 10: no change, no persistence.
        viewModel.upgradeStat("str", earnedCoins = 10)
        advanceUntilIdle()

        val sheet = (viewModel.uiState as CharacterUiState.Content).sheet
        assertEquals(5, sheet.stats.first { it.key == "str" }.value)
        assertEquals(0, viewModel.spentCoins)
        assertEquals(0, repo.updateCount)
    }

    @Test
    fun upgradeStat_doesNothing_atMaxValue() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeCharacterDataSource(sheetWith(strength = CHARACTER_STAT_MAX))
        val viewModel = CharacterViewModel(
            Application(), repo, InMemoryCoinWalletRepository(), mainDispatcherRule.dispatcher
        )
        viewModel.loadCharacter()
        viewModel.setUser("user@example.com")
        advanceUntilIdle()

        viewModel.upgradeStat("str", earnedCoins = Int.MAX_VALUE)
        advanceUntilIdle()

        val sheet = (viewModel.uiState as CharacterUiState.Content).sheet
        assertEquals(CHARACTER_STAT_MAX, sheet.stats.first { it.key == "str" }.value)
        assertEquals(0, repo.updateCount)
    }

    @Test
    fun applyLevelUpRewards_increasesTotalPowerByLevelsGained() {
        val sheet = sheetWith(strength = 5).copy(
            skills = listOf(CharacterSkill("Лесоруб", level = 1, progress = 0f, key = "wood"))
        )
        val before = totalPower(sheet)

        val rewarded = applyLevelUpRewards(sheet, levelsGained = 3, random = Random(42))

        assertEquals(before + 3, totalPower(rewarded))
    }

    @Test
    fun applyLevelUpRewards_atStatCap_rewardsSkillInstead() {
        val sheet = CharacterSheet(
            name = "Hero", level = 1, xp = 0, xpToNext = 100, portraitRes = 0,
            stats = listOf(CharacterStat("Сила", "", CHARACTER_STAT_MAX, key = "str")),
            skills = listOf(CharacterSkill("Лесоруб", level = 1, progress = 0f, key = "wood"))
        )

        val rewarded = applyLevelUpRewards(sheet, levelsGained = 5, random = Random(7))

        assertEquals(CHARACTER_STAT_MAX, rewarded.stats.first().value)
        assertEquals(6, rewarded.skills.first().level)
    }

    @Test
    fun grantXp_onLevelUp_appliesRandomRewardAndPersists() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeCharacterDataSource(sheetWith(strength = 5), levelGainOnXp = 1)
        val viewModel = CharacterViewModel(
            Application(), repo, InMemoryCoinWalletRepository(), mainDispatcherRule.dispatcher
        )
        viewModel.loadCharacter()
        advanceUntilIdle()
        val before = totalPower((viewModel.uiState as CharacterUiState.Content).sheet)

        viewModel.grantXp(characterXp = 100, skillKey = null, skillXp = 0)
        advanceUntilIdle()

        val sheet = (viewModel.uiState as CharacterUiState.Content).sheet
        assertEquals(2, sheet.level)
        assertEquals(before + 1, totalPower(sheet))
        assertEquals(1, repo.updateCount)
    }

    private fun totalPower(sheet: CharacterSheet): Int =
        sheet.stats.sumOf { it.value } + sheet.skills.sumOf { it.level }

    private class FakeCharacterDataSource(
        initial: CharacterSheet,
        private val levelGainOnXp: Int = 0
    ) : CharacterDataSource {
        private var current = initial
        var updateCount = 0
            private set

        override suspend fun getCharacter(): Result<CharacterSheet> = Result.success(current)

        override suspend fun updateCharacter(sheet: CharacterSheet): Result<CharacterSheet> {
            updateCount++
            current = sheet
            return Result.success(sheet)
        }

        override suspend fun addExperience(
            characterXp: Int,
            skillKey: String?,
            skillXp: Int
        ): Result<CharacterSheet> {
            current = current.copy(level = current.level + levelGainOnXp)
            return Result.success(current)
        }
    }
}
