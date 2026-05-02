package com.example.mutlabocsnotes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mutlabocsnotes.network.AuthApi
import com.example.mutlabocsnotes.network.HomeCardsApi
import com.example.mutlabocsnotes.network.NotesApi

// Единая точка создания зависимостей приложения.
// В экранах и ViewModel зависимости только используются, но больше не создаются напрямую.
class AppContainer(
    private val application: Application
) {
    // Один общий менеджер сессии нужен и репозиториям, и сетевому слою.
    val sessionManager: SessionManager by lazy {
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
            api = authApi
        )
    }

    val notesRepository: NotesDataSource by lazy {
        NotesRepository(
            api = authenticatedRetrofit.create(NotesApi::class.java)
        )
    }

    val homeInfoRepository: HomeInfoDataSource by lazy {
        HomeInfoRepository(
            api = authenticatedRetrofit.create(HomeCardsApi::class.java)
        )
    }

    val deadlineNotificationScheduler: DeadlineScheduler by lazy {
        DeadlineNotificationScheduler(application)
    }

    // Фабрика создаёт root ViewModel с зависимостями из контейнера.
    val viewModelFactory: ViewModelProvider.Factory by lazy {
        MutlabocNotesViewModelFactory(
            application = application,
            authRepository = authRepository,
            notesRepository = notesRepository,
            homeInfoRepository = homeInfoRepository,
            deadlineNotificationScheduler = deadlineNotificationScheduler
        )
    }
}

// Ручная фабрика ViewModel вместо DI-фреймворка: все зависимости берутся из AppContainer.
class MutlabocNotesViewModelFactory(
    private val application: Application,
    private val authRepository: AuthSessionRepository,
    private val notesRepository: NotesDataSource,
    private val homeInfoRepository: HomeInfoDataSource,
    private val deadlineNotificationScheduler: DeadlineScheduler
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

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
