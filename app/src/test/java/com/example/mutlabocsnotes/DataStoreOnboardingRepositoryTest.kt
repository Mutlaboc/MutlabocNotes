package com.example.mutlabocsnotes

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreOnboardingRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultStateHasNothingSeen() = runTest {
        val store = repository(preferenceFile())

        val state = store.repository.state(USER).first()

        assertFalse(state.welcomeSeen)
        assertFalse(state.addNoteHintSeen)
        assertFalse(state.homeInfoHintSeen)
        assertFalse(state.completedHintSeen)

        store.scope.cancel()
    }

    @Test
    fun welcomeSeenSurvivesRepositoryRecreation() = runTest {
        val file = preferenceFile()
        var store = repository(file)

        store.repository.markWelcomeSeen(USER)
        assertTrue(store.repository.state(USER).first().welcomeSeen)
        store.scope.cancel()

        store = repository(file)
        assertTrue(store.repository.state(USER).first().welcomeSeen)

        store.scope.cancel()
    }

    @Test
    fun eachHintIsPersistedIndependently() = runTest {
        val store = repository(preferenceFile())

        store.repository.markHintSeen(USER, OnboardingHintStep.HOME_INFO)
        val state = store.repository.state(USER).first()

        assertFalse(state.addNoteHintSeen)
        assertTrue(state.homeInfoHintSeen)
        assertFalse(state.completedHintSeen)

        store.scope.cancel()
    }

    @Test
    fun progressIsScopedPerUser() = runTest {
        val store = repository(preferenceFile())

        store.repository.markWelcomeSeen(USER)

        assertTrue(store.repository.state(USER).first().welcomeSeen)
        assertFalse(store.repository.state(OTHER_USER).first().welcomeSeen)

        store.scope.cancel()
    }

    private fun preferenceFile(): File {
        return File(temporaryFolder.root, "onboarding.preferences_pb")
    }

    private fun repository(file: File): TestOnboardingStore {
        val scope = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            file
        }
        return TestOnboardingStore(
            repository = DataStoreOnboardingRepository(dataStore),
            scope = scope
        )
    }

    private data class TestOnboardingStore(
        val repository: DataStoreOnboardingRepository,
        val scope: CoroutineScope
    )

    private companion object {
        const val USER = "user@example.com"
        const val OTHER_USER = "other@example.com"
    }
}
