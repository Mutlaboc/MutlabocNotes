package app.homenotes.android

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.homenotes.android.network.AchievementsApi
import app.homenotes.android.network.AuthApi
import app.homenotes.android.network.CharacterApi
import app.homenotes.android.network.EventsApi
import app.homenotes.android.network.HomeCardsApi
import app.homenotes.android.network.InventoryApi
import app.homenotes.android.network.NotesApi
import app.homenotes.android.local.HomeNotesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Единая точка создания зависимостей приложения.
// В экранах и ViewModel зависимости только используются, но больше не создаются напрямую.
class AppContainer(
    private val application: Application
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: HomeNotesDatabase by lazy { HomeNotesDatabase.create(application) }
    private val offlineDao by lazy { database.offlineDao() }

    val syncCoordinator: WorkManagerSyncCoordinator by lazy {
        WorkManagerSyncCoordinator(application, offlineDao)
    }
    // Один общий store сессии нужен и репозиториям, и сетевому слою.
    val sessionManager: AuthSessionStore by lazy {
        SessionManager(application)
    }

    // Auth API не добавляет Bearer-токен сам, потому что используется для входа и обновления сессии.
    private val authApi: AuthApi by lazy {
        AuthRepository.createAuthApi(ApiConfig.BASE_URL)
    }

    // Этот Retrofit уже знает про текущую сессию и добавляет авторизацию в защищённые запросы.
    private val authenticatedRetrofit by lazy {
        AuthenticatedApiFactory.createRetrofit(
            sessionManager = sessionManager,
            baseUrl = ApiConfig.BASE_URL
        )
    }

    val authRepository: AuthSessionRepository by lazy {
        AuthRepository(
            sessionManager = sessionManager,
            api = authApi,
            onSessionAvailable = { email ->
                normalizeAccountKey(email)?.let { account ->
                    applicationScope.launch {
                        coinWalletRepository.spentCoins(email)
                        syncCoordinator.activateAccount(account)
                    }
                }
            },
            onSessionCleared = { email ->
                normalizeAccountKey(email)?.let(syncCoordinator::cancel)
            },
        )
    }

    private val notesApi: NotesApi by lazy { authenticatedRetrofit.create(NotesApi::class.java) }
    private val cardsApi: HomeCardsApi by lazy { authenticatedRetrofit.create(HomeCardsApi::class.java) }
    private val characterApi: CharacterApi by lazy { authenticatedRetrofit.create(CharacterApi::class.java) }
    private val inventoryApi: InventoryApi by lazy { authenticatedRetrofit.create(InventoryApi::class.java) }
    private val eventsApi: EventsApi by lazy { authenticatedRetrofit.create(EventsApi::class.java) }
    private val achievementsApi: AchievementsApi by lazy { authenticatedRetrofit.create(AchievementsApi::class.java) }

    val syncEngine: OfflineSyncEngine by lazy {
        OfflineSyncEngine(
            offlineDao, notesApi, cardsApi, characterApi, inventoryApi, eventsApi, sessionManager,
            achievementsApi,
        )
    }

    val achievementsTracker: AchievementsTracker by lazy {
        AchievementsTracker(offlineDao, sessionManager, applicationScope, syncCoordinator)
    }

    val achievementsRepository: AchievementsRepository by lazy {
        AchievementsRepository(offlineDao, sessionManager)
    }

    val notesRepository: NotesDataSource by lazy {
        OfflineNotesRepository(offlineDao, sessionManager, syncCoordinator, achievementsTracker)
    }

    val homeInfoRepository: HomeInfoDataSource by lazy {
        OfflineHomeInfoRepository(offlineDao, sessionManager, syncCoordinator, achievementsTracker)
    }

    val characterRepository: CharacterDataSource by lazy {
        OfflineCharacterRepository(offlineDao, sessionManager, syncCoordinator, achievementsTracker)
    }

    val inventoryRepository: InventoryDataSource by lazy {
        OfflineInventoryRepository(offlineDao, sessionManager, syncCoordinator, achievementsTracker)
    }

    val focusEventsRepository: FocusEventsRepository by lazy {
        FocusEventsRepository(offlineDao, sessionManager, syncCoordinator, achievements = achievementsTracker)
    }

    val deadlineNotificationScheduler: DeadlineScheduler by lazy {
        DeadlineNotificationScheduler(application)
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(application)
    }

    val onboardingRepository: OnboardingRepository by lazy {
        DataStoreOnboardingRepository(application)
    }

    val coinWalletRepository: CoinWalletRepository by lazy {
        RoomCoinWalletRepository(offlineDao, DataStoreCoinWalletRepository(application), achievementsTracker)
    }

    // Фабрика создаёт root ViewModel с зависимостями из контейнера.
    val viewModelFactory: ViewModelProvider.Factory by lazy {
        HomeNotesViewModelFactory(
            application = application,
            authRepository = authRepository,
            notesRepository = notesRepository,
            homeInfoRepository = homeInfoRepository,
            characterRepository = characterRepository,
            inventoryRepository = inventoryRepository,
            deadlineNotificationScheduler = deadlineNotificationScheduler,
            settingsRepository = settingsRepository,
            onboardingRepository = onboardingRepository,
            coinWalletRepository = coinWalletRepository,
            focusEventsRepository = focusEventsRepository,
            achievementsRepository = achievementsRepository,
            achievementsTracker = achievementsTracker
        )
    }
}

// Ручная фабрика ViewModel вместо DI-фреймворка: все зависимости берутся из AppContainer.
class HomeNotesViewModelFactory(
    private val application: Application,
    private val authRepository: AuthSessionRepository,
    private val notesRepository: NotesDataSource,
    private val homeInfoRepository: HomeInfoDataSource,
    private val characterRepository: CharacterDataSource,
    private val inventoryRepository: InventoryDataSource,
    private val deadlineNotificationScheduler: DeadlineScheduler,
    private val settingsRepository: SettingsRepository,
    private val onboardingRepository: OnboardingRepository = InMemoryOnboardingRepository(),
    private val coinWalletRepository: CoinWalletRepository = InMemoryCoinWalletRepository(),
    private val focusEventsRepository: FocusEventsRepository? = null,
    private val achievementsRepository: AchievementsRepository? = null,
    private val achievementsTracker: AchievementsTracker? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Каждая ViewModel получает готовые зависимости и не знает, как они создаются.
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(
                application = application,
                repository = authRepository,
                autoRestore = true
            ) as T

            modelClass.isAssignableFrom(NotesViewModel::class.java) -> NotesViewModel(
                application = application,
                repository = notesRepository,
                notificationScheduler = deadlineNotificationScheduler
            ) as T

            modelClass.isAssignableFrom(HomeInfoViewModel::class.java) -> HomeInfoViewModel(
                application = application,
                repository = homeInfoRepository
            ) as T

            modelClass.isAssignableFrom(CharacterViewModel::class.java) -> CharacterViewModel(
                application = application,
                repository = characterRepository,
                coinWallet = coinWalletRepository
            ) as T

            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> InventoryViewModel(
                application = application,
                repository = inventoryRepository
            ) as T

            modelClass.isAssignableFrom(FocusEventsViewModel::class.java) -> FocusEventsViewModel(
                application = application,
                repository = requireNotNull(focusEventsRepository) {
                    "FocusEventsRepository is required for FocusEventsViewModel"
                }
            ) as T

            modelClass.isAssignableFrom(AchievementsViewModel::class.java) -> AchievementsViewModel(
                application = application,
                repository = requireNotNull(achievementsRepository) {
                    "AchievementsRepository is required for AchievementsViewModel"
                },
                tracker = achievementsTracker
            ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                application = application,
                repository = settingsRepository
            ) as T

            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(
                application = application,
                repository = onboardingRepository
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
