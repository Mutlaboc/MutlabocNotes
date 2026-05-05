package com.example.mutlabocsnotes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        // Activity получает готовую фабрику из Application и передаёт её в Compose-root.
        val appContainer = (application as MutlabocNotesApplication).appContainer
        setContent {
            MyApp(viewModelFactory = appContainer.viewModelFactory)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.deadline_notification_channel_name)
            val descriptionText = getString(R.string.deadline_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(DeadlineNotification.CHANNEL_ID, name, importance)
                .apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager? = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MyApp(
    viewModelFactory: ViewModelProvider.Factory,
    // Все root ViewModel создаются одной фабрикой, чтобы зависимости не собирались внутри UI.
    authViewModel: AuthViewModel = viewModel(factory = viewModelFactory),
    notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory),
    homeInfoViewModel: HomeInfoViewModel = viewModel(factory = viewModelFactory)
) {
    val navController = rememberNavController()
    val authUiState = authViewModel.uiState
    val authState = authUiState.authState

    LaunchedEffect(Unit) {
        SessionEventBus.events.collect { event ->
            when (event) {
                SessionEvent.SessionExpired -> authViewModel.handleSessionExpired()
            }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                notesViewModel.loadNotes()
                navController.navigate("home") {
                    popUpTo("bootstrap") { inclusive = false }
                    launchSingleTop = true
                }
            }

            is AuthState.Unauthenticated -> {
                navController.navigate("auth") {
                    popUpTo("bootstrap") { inclusive = false }
                    launchSingleTop = true
                }
            }

            AuthState.Checking -> Unit
        }
    }

    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colors = if (isDarkTheme) darkColors() else lightColors()) {
        NavHost(
            navController = navController,
            startDestination = "bootstrap"
        ) {
            composable("bootstrap") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            composable("auth") {
                AuthScreen(
                    uiState = authUiState,
                    onSignIn = authViewModel::signIn,
                    onSignUp = authViewModel::signUp,
                    onGoogleIdToken = authViewModel::signInWithGoogle,
                    onYandexAccessToken = authViewModel::signInWithYandex,
                    onClearError = authViewModel::clearError
                )
            }

            composable("home") {
                HomeScreen(
                    notes = notesViewModel.notes,
                    totalCoins = notesViewModel.totalCoins,
                    userEmail = authUiState.currentEmail,
                    isLoading = notesViewModel.isLoading,
                    errorMessage = notesViewModel.errorMessage,
                    onRetryNotes = notesViewModel::loadNotes,
                    onAddNoteClick = {
                        navController.navigate("edit")
                    },
                    onNoteClick = { noteId ->
                        navController.navigate("edit/$noteId")
                    },
                    onOtherCellClick = { index ->
                        when (index) {
                            0 -> navController.navigate("completed") {
                                launchSingleTop = true
                            }
                            2 -> navController.navigate("home_info")
                        }
                    },
                    onCompletionChange = { noteId, isCompleted ->
                        notesViewModel.setNoteCompletion(noteId, isCompleted)
                    },
                    onSwitchUser = authViewModel::logout,
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDarkTheme = it },
                    onLogout = authViewModel::logout,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("completed") {
                CompletedNotesScreen(
                    notes = notesViewModel.notes,
                    errorMessage = notesViewModel.errorMessage,
                    onRetryNotes = notesViewModel::loadNotes,
                    onaddNoteClick = {
                        navController.navigate("edit")
                    },
                    onNoteClick = { noteId ->
                        navController.navigate("edit/$noteId")
                    },
                    onCompletionChange = { noteId, isCompleted ->
                        notesViewModel.setNoteCompletion(noteId, isCompleted)
                    },
                    onNavigateHome = {
                        navController.popBackStack()
                    }
                )
            }

            composable("home_info") {
                LaunchedEffect(Unit) {
                    homeInfoViewModel.loadCards()
                }
                HomeInfoScreen(
                    cards = homeInfoViewModel.cards,
                    isLoading = homeInfoViewModel.isLoading,
                    errorMessage = homeInfoViewModel.errorMessage,
                    onAddClick = { navController.navigate("home_info_edit") },
                    onCardClick = { cardId ->
                        navController.navigate("home_info_edit/$cardId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("home_info_edit") {
                EditHomeInfoCardScreen(
                    card = null,
                    onSaveClick = { card ->
                        homeInfoViewModel.addCard(card)
                        navController.popBackStack()
                    },
                    onDeleteClick = null,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "home_info_edit/{cardId}",
                arguments = listOf(navArgument("cardId") { type = NavType.StringType })
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                val card = homeInfoViewModel.cards.find { it.id == cardId }
                EditHomeInfoCardScreen(
                    card = card,
                    onSaveClick = { updatedCard ->
                        homeInfoViewModel.updateCard(updatedCard)
                        navController.popBackStack()
                    },
                    onDeleteClick = {
                        homeInfoViewModel.deleteCard(cardId)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("edit") {
                EditNoteScreen(
                    note = null,
                    onSaveClick = { createdNote ->
                        notesViewModel.addNote(createdNote)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments!!.getString("noteId") ?: ""
                val note = notesViewModel.notes.find { it.id == noteId }
                EditNoteScreen(
                    note = note,
                    onSaveClick = { updatedNote ->
                        if (note != null) {
                            notesViewModel.updateNote(updatedNote)
                        }
                        navController.popBackStack()
                    },
                    onDeleteClick = {
                        if (note != null) {
                            notesViewModel.deleteNote(noteId)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
