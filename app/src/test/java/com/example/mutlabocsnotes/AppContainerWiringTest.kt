package com.example.mutlabocsnotes

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

// Регрессионные проверки DI-wiring без Robolectric: читаем исходники и манифест как обычные файлы.
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainerWiringTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun manifest_registersCustomApplication() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(
            manifest.contains("""android:name=".MutlabocNotesApplication"""")
        )
    }

    @Test
    fun manifest_declaresScheduleExactAlarmOnly() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.USE_EXACT_ALARM"))
    }

    @Test
    fun appContainer_ownsSharedDependencyCreation() {
        val appContainer = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/AppContainer.kt"
        ).readText()

        assertTrue(appContainer.contains("val sessionManager: AuthSessionStore by lazy"))
        assertTrue(appContainer.contains("val authRepository: AuthSessionRepository by lazy"))
        assertTrue(appContainer.contains("val notesRepository: NotesDataSource by lazy"))
        assertTrue(appContainer.contains("val homeInfoRepository: HomeInfoDataSource by lazy"))
        assertTrue(appContainer.contains("val deadlineNotificationScheduler: DeadlineScheduler by lazy"))
        assertTrue(appContainer.contains("val settingsRepository: SettingsRepository by lazy"))
        assertTrue(appContainer.contains("class MutlabocNotesViewModelFactory"))
    }

    @Test
    fun viewModelFactory_createsRootViewModels() = runTest(mainDispatcherRule.dispatcher) {
        val factory = MutlabocNotesViewModelFactory(
            application = Application(),
            authRepository = WiringFakeAuthSessionRepository(),
            notesRepository = WiringFakeNotesDataSource(),
            homeInfoRepository = WiringFakeHomeInfoDataSource(),
            deadlineNotificationScheduler = WiringFakeDeadlineScheduler(),
            settingsRepository = WiringFakeSettingsRepository()
        )

        assertNotNull(factory.create(AuthViewModel::class.java))
        assertNotNull(factory.create(NotesViewModel::class.java))
        assertNotNull(factory.create(HomeInfoViewModel::class.java))
        assertNotNull(factory.create(SettingsViewModel::class.java))
        advanceUntilIdle()
    }

    @Test
    fun myApp_usesFactoryAndDoesNotCreateSessionManagerDirectly() {
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()
        val myAppBody = mainActivity.substringAfter("fun MyApp(")

        assertTrue(myAppBody.contains("viewModelFactory: ViewModelProvider.Factory"))
        assertTrue(myAppBody.contains("viewModel(factory = viewModelFactory)"))
        assertFalse(myAppBody.contains("SessionManager("))
    }

    @Test
    fun duplicateAuthLayer_isNotReferencedFromSources() {
        // Держим старый auth-пакет удалённым, чтобы UI не вернулся к legacy repository.
        val duplicateRepository = "BackendAuth" + "Repository"
        val duplicatePackage = listOf(
            "com",
            "example",
            "mutlabocsnotes",
            "auth"
        ).joinToString(".")
        val checkedSources = kotlinSourceFiles()
            .filterNot { it.name == "AppContainerWiringTest.kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(checkedSources.contains(duplicateRepository))
        assertFalse(checkedSources.contains(duplicatePackage))
        assertFalse(projectFile("app/src/main/java/com/example/mutlabocsnotes/auth").exists())
    }

    @Test
    fun rootNavigation_usesUnifiedAuthViewModelCallbacks() {
        // Smoke-проверка root wiring без Compose runner: auth callbacks должны идти через единую ViewModel.
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()
        val myAppBody = mainActivity.substringAfter("fun MyApp(")

        assertTrue(myAppBody.contains("authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)"))
        assertTrue(myAppBody.contains("onSignIn = authViewModel::signIn"))
        assertTrue(myAppBody.contains("onSignUp = authViewModel::signUp"))
        assertTrue(myAppBody.contains("onGoogleIdToken = authViewModel::signInWithGoogle"))
        assertTrue(myAppBody.contains("onYandexAccessToken = authViewModel::signInWithYandex"))
        assertTrue(myAppBody.contains("is AuthState.Authenticated ->"))
        assertTrue(myAppBody.contains("""navController.navigate("home")"""))
        assertTrue(myAppBody.contains("is AuthState.Unauthenticated ->"))
        assertTrue(myAppBody.contains("""navController.navigate("auth")"""))
        assertTrue(myAppBody.contains("AuthState.Checking -> Unit"))
    }

    @Test
    fun rootNavigation_handlesExactAlarmSnackbarAction() {
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()

        assertTrue(mainActivity.contains("UiMessageAction.OPEN_EXACT_ALARM_SETTINGS"))
        assertTrue(mainActivity.contains("Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM"))
        assertTrue(mainActivity.contains("exactAlarmSettingsLauncher.launch"))
        assertTrue(mainActivity.contains("notesViewModel.loadNotes()"))
        assertTrue(mainActivity.contains("onMessageAction = onMessageAction"))
    }

    @Test
    fun rootNavigation_clearsNotesAndSchedulesWhenUnauthenticated() {
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()

        assertTrue(mainActivity.contains("is AuthState.Unauthenticated ->"))
        assertTrue(mainActivity.contains("notesViewModel.clearAll()"))
    }

    @Test
    fun rootNavigation_handlesPendingNotificationNoteAfterLoadingNotes() {
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()

        assertTrue(mainActivity.contains("DeadlineNotification.noteIdFromIntent(intent)"))
        assertTrue(mainActivity.contains("override fun onNewIntent(intent: Intent)"))
        assertTrue(mainActivity.contains("pendingNotificationNoteId: String? = null"))
        assertTrue(mainActivity.contains("onPendingNotificationHandled(targetNoteId)"))
        assertTrue(mainActivity.contains("state.notes.any { it.id == targetNoteId }"))
        assertTrue(mainActivity.contains("navController.navigate(\"edit/\${Uri.encode(targetNoteId)}\")"))
    }

    @Test
    fun deadlineNotificationContentIntentTargetsTheDeadlineNote() {
        val receiver = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/DeadlineNotificationReceiver.kt"
        ).readText()

        assertTrue(receiver.contains("const val ACTION_OPEN_NOTE"))
        assertTrue(receiver.contains("fun openNoteIntent(context: Context, noteId: String): Intent"))
        assertTrue(receiver.contains("action = ACTION_OPEN_NOTE"))
        assertTrue(receiver.contains("data = openNoteUri(noteId)"))
        assertTrue(receiver.contains("putExtra(EXTRA_NOTE_ID, noteId)"))
        assertTrue(receiver.contains("PendingIntent.getActivity"))
        assertTrue(receiver.contains("DeadlineNotification.requestCodeForId(noteId)"))
    }

    @Test
    fun settingsPersistence_isWiredThroughRootViewModel() {
        val appContainer = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/AppContainer.kt"
        ).readText()
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()
        val settingsScreen = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/SettingsScreen.kt"
        ).readText()

        assertTrue(appContainer.contains("DataStoreSettingsRepository(application)"))
        assertTrue(appContainer.contains("SettingsViewModel("))
        assertTrue(mainActivity.contains("settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)"))
        assertTrue(mainActivity.contains("settingsViewModel.uiState.preferences"))
        assertTrue(mainActivity.contains("settingsViewModel::setDarkTheme"))
        assertTrue(mainActivity.contains("settingsViewModel::setLanguage"))
        assertFalse(mainActivity.contains("var isDarkTheme by rememberSaveable"))
        assertFalse(settingsScreen.contains("rememberSaveable"))
    }

    private fun projectFile(path: String): File {
        val userDir = checkNotNull(System.getProperty("user.dir")) {
            "user.dir is not set"
        }
        var directory = File(userDir)
        while (!File(directory, "settings.gradle.kts").exists()) {
            directory = checkNotNull(directory.parentFile) {
                "Could not locate project root"
            }
        }
        return File(directory, path)
    }

    private fun kotlinSourceFiles(): List<File> {
        // Source-level тесты читают Kotlin-файлы напрямую, поэтому не требуют Android instrumentation.
        val root = projectFile(".")
        return listOf(
            File(root, "app/src/main/java"),
            File(root, "app/src/test/java")
        ).flatMap { sourceRoot ->
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        }
    }
}

private class WiringFakeAuthSessionRepository : AuthSessionRepository {
    override suspend fun login(email: String, password: String): Result<AuthorizedSession> =
        Result.success(AuthorizedSession(email))

    override suspend fun register(email: String, password: String): Result<AuthorizedSession> =
        Result.success(AuthorizedSession(email))

    override suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession> =
        Result.success(AuthorizedSession("google@example.com"))

    override suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession> =
        Result.success(AuthorizedSession("yandex@example.com"))

    override suspend fun restoreSession(): Result<AuthorizedSession> =
        Result.failure(IllegalStateException("No saved access token"))

    override suspend fun logout() = Unit

    override fun clearLocalSession() = Unit
}

private class WiringFakeNotesDataSource : NotesDataSource {
    override suspend fun getAllNotes(): Result<List<Note>> = Result.success(emptyList())
    override suspend fun insert(note: Note): Result<String> = Result.success(note.id.ifBlank { "note-id" })
    override suspend fun update(note: Note): Result<Unit> = Result.success(Unit)
    override suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun delete(noteId: String): Result<Unit> = Result.success(Unit)
}

private class WiringFakeHomeInfoDataSource : HomeInfoDataSource {
    override suspend fun getAllCards(): Result<List<HomeInfoCard>> = Result.success(emptyList())
    override suspend fun insert(card: HomeInfoCard): Result<String> = Result.success("card-id")
    override suspend fun update(card: HomeInfoCard): Result<Unit> = Result.success(Unit)
    override suspend fun delete(cardId: String): Result<Unit> = Result.success(Unit)
}

private class WiringFakeDeadlineScheduler : DeadlineScheduler {
    override fun schedule(note: Note): DeadlineScheduleResult = DeadlineScheduleResult.ScheduledExact
    override fun cancel(noteId: String) = Unit
    override fun scheduleAll(notes: List<Note>): DeadlineScheduleResult = DeadlineScheduleResult.ScheduledExact
    override fun cancelAll() = Unit
}

private class WiringFakeSettingsRepository : SettingsRepository {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences())
    override suspend fun setDarkTheme(isDarkTheme: Boolean) = Unit
    override suspend fun setLanguage(language: AppLanguage) = Unit
}
