package app.homenotes.android

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
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
    private var pendingNotificationNoteId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationNoteId = DeadlineNotification.noteIdFromIntent(intent)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        // Activity получает готовую фабрику из Application и передаёт её в Compose-root.
        val appContainer = (application as HomeNotesApplication).appContainer
        setContent {
            val syncStatus by appContainer.syncCoordinator.status.collectAsState()
            MyApp(
                viewModelFactory = appContainer.viewModelFactory,
                syncStatus = syncStatus,
                onRetrySync = {
                    normalizeAccountKey(appContainer.sessionManager.getEmail())
                        ?.let(appContainer.syncCoordinator::request)
                },
                pendingNotificationNoteId = pendingNotificationNoteId,
                onPendingNotificationHandled = { handledNoteId ->
                    if (pendingNotificationNoteId == handledNoteId) {
                        pendingNotificationNoteId = null
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationNoteId = DeadlineNotification.noteIdFromIntent(intent)
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

internal fun exactAlarmSettingsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:$packageName")
    }
}

internal fun applicationDetailsSettingsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
}

internal fun homeInfoLinkIntent(link: String): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(link))
}

@Composable
fun MyApp(
    viewModelFactory: ViewModelProvider.Factory,
    syncStatus: SyncStatus = SyncStatus.Idle,
    onRetrySync: () -> Unit = {},
    pendingNotificationNoteId: String? = null,
    onPendingNotificationHandled: (String) -> Unit = {},
    // Все root ViewModel создаются одной фабрикой, чтобы зависимости не собирались внутри UI.
    authViewModel: AuthViewModel = viewModel(factory = viewModelFactory),
    notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory),
    homeInfoViewModel: HomeInfoViewModel = viewModel(factory = viewModelFactory),
    settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory),
    characterViewModel: CharacterViewModel = viewModel(factory = viewModelFactory),
    focusEventsViewModel: FocusEventsViewModel = viewModel(factory = viewModelFactory),
    inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory),
    onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory),
    achievementsViewModel: AchievementsViewModel = viewModel(factory = viewModelFactory)
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authUiState = authViewModel.uiState
    val authState = authUiState.authState
    val notesUiState = notesViewModel.uiState
    val settingsPreferences = settingsViewModel.uiState.preferences
    var isWaitingForNotificationNotes by rememberSaveable { mutableStateOf(false) }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notesViewModel.loadNotes()
    }
    val onMessageAction: (UiMessageAction) -> Unit = { action ->
        when (action) {
            UiMessageAction.OPEN_EXACT_ALARM_SETTINGS -> {
                try {
                    exactAlarmSettingsLauncher.launch(exactAlarmSettingsIntent(context.packageName))
                } catch (error: ActivityNotFoundException) {
                    exactAlarmSettingsLauncher.launch(applicationDetailsSettingsIntent(context.packageName))
                }
            }
        }
    }

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
                isWaitingForNotificationNotes = pendingNotificationNoteId != null
                onboardingViewModel.setUser(authState.email)
                characterViewModel.setUser(authState.email)
                achievementsViewModel.setUser(authState.email)
                notesViewModel.loadNotes()
                navController.navigate("home") {
                    popUpTo("bootstrap") { inclusive = false }
                    launchSingleTop = true
                }
            }

            is AuthState.Unauthenticated -> {
                isWaitingForNotificationNotes = false
                onboardingViewModel.setUser(null)
                characterViewModel.setUser(null)
                achievementsViewModel.setUser(null)
                notesViewModel.clearAll()
                homeInfoViewModel.clearAll()
                navController.navigate("auth") {
                    popUpTo("bootstrap") { inclusive = false }
                    launchSingleTop = true
                }
            }

            AuthState.Checking -> Unit
        }
    }

    LaunchedEffect(pendingNotificationNoteId) {
        if (pendingNotificationNoteId != null && authState is AuthState.Authenticated) {
            isWaitingForNotificationNotes = true
            notesViewModel.loadNotes()
            navController.navigate("home") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(authState, notesUiState, pendingNotificationNoteId, isWaitingForNotificationNotes) {
        when (
            val decision = pendingNotificationNavigationDecision(
                authState = authState,
                notesUiState = notesUiState,
                pendingNotificationNoteId = pendingNotificationNoteId,
                isWaitingForNotificationNotes = isWaitingForNotificationNotes
            )
        ) {
            is PendingNotificationNavigationDecision.OpenNote -> {
                isWaitingForNotificationNotes = false
                onPendingNotificationHandled(decision.noteId)
                navController.navigate("edit/${Uri.encode(decision.noteId)}") {
                    launchSingleTop = true
                }
            }
            is PendingNotificationNavigationDecision.ClearPending -> {
                isWaitingForNotificationNotes = false
                onPendingNotificationHandled(decision.noteId)
            }
            PendingNotificationNavigationDecision.Wait -> Unit
        }
    }

    MaterialTheme(colors = if (settingsPreferences.isDarkTheme) cozyDarkColors() else cozyLightColors()) {
        Box(Modifier.fillMaxSize()) {
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
                    onGoogleTokenEmpty = authViewModel::onGoogleTokenEmpty,
                    onGoogleSignInFailed = authViewModel::onGoogleSignInFailed,
                    onYandexTokenEmpty = authViewModel::onYandexTokenEmpty,
                    onYandexSignInFailed = authViewModel::onYandexSignInFailed,
                    onYandexSignInCancelled = authViewModel::onYandexSignInCancelled,
                    onMessageShown = authViewModel::onMessageShown,
                    onClearInlineError = authViewModel::clearInlineError
                )
            }

            composable("home") {
                LaunchedEffect(Unit) { characterViewModel.loadCharacter() }
                HomeScreen(
                    uiState = notesViewModel.uiState,
                    uiMessage = notesViewModel.uiMessage,
                    userEmail = authUiState.currentEmail,
                    characterSheet = (characterViewModel.uiState as? CharacterUiState.Content)?.sheet,
                    onRetryNotes = notesViewModel::loadNotes,
                    onMessageShown = notesViewModel::onMessageShown,
                    onMessageAction = onMessageAction,
                    onAddNoteClick = {
                        navController.navigate("edit")
                    },
                    onNoteClick = { noteId ->
                        navController.navigate("edit/${Uri.encode(noteId)}")
                    },
                    onCompletedNotesClick = {
                        navController.navigate("completed") {
                            launchSingleTop = true
                        }
                    },
                    onHomeInfoClick = {
                        navController.navigate("home_info")
                    },
                    onCompletionChange = { noteId, isCompleted ->
                        notesViewModel.setNoteCompletion(noteId, isCompleted)
                    },
                    onChecklistItemToggle = { noteId, index, checked ->
                        notesViewModel.toggleChecklistItem(noteId, index, checked)
                    },
                    newlyCreatedNoteId = notesViewModel.lastCreatedNoteId,
                    onNewNoteShown = notesViewModel::onNewNoteShown,
                    onSwitchUser = authViewModel::logout,
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenCharacter = {
                        navController.navigate("character")
                    },
                    focusEventsFeed = focusEventsViewModel.feed,
                    onFocusSessionStart = focusEventsViewModel::startSession,
                    onFocusMinuteTick = { skillKey ->
                        focusEventsViewModel.onMinuteTick(skillKey)
                    },
                    onboarding = onboardingViewModel.uiState,
                    onWelcomeSeen = onboardingViewModel::markWelcomeSeen,
                    onHintSeen = onboardingViewModel::markHintSeen
                )
            }

            composable("character") {
                LaunchedEffect(Unit) { characterViewModel.loadCharacter() }
                val earnedCoins = (notesViewModel.uiState as? NotesUiState.Content)?.totalCoins ?: 0
                val availableCoins = (earnedCoins - characterViewModel.spentCoins).coerceAtLeast(0)
                CharacterScreen(
                    uiState = characterViewModel.uiState,
                    onBack = { navController.popBackStack() },
                    onRetry = { characterViewModel.loadCharacter() },
                    onRename = { newName -> characterViewModel.updateName(newName) },
                    availableCoins = availableCoins,
                    onUpgradeStat = { statKey -> characterViewModel.upgradeStat(statKey, earnedCoins) },
                    onOpenInventory = { navController.navigate("inventory") },
                    achievementPoints = achievementsViewModel.totalPoints,
                    onOpenAchievements = { navController.navigate("achievements") }
                )
            }

            composable("inventory") {
                LaunchedEffect(Unit) { inventoryViewModel.loadInventory() }
                InventoryScreen(
                    uiState = inventoryViewModel.uiState,
                    onBack = { navController.popBackStack() },
                    onRetry = { inventoryViewModel.loadInventory() },
                    onEquip = { itemId -> inventoryViewModel.equip(itemId) },
                    onUnequip = { slot -> inventoryViewModel.unequip(slot) }
                )
            }

            composable("achievements") {
                AchievementsScreen(
                    items = achievementsViewModel.screenItems,
                    totalPoints = achievementsViewModel.totalPoints,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    isDarkTheme = settingsPreferences.isDarkTheme,
                    onThemeChange = settingsViewModel::setDarkTheme,
                    selectedLanguage = settingsPreferences.language,
                    availableLanguages = AppLanguage.entries.toList(),
                    onLanguageChange = settingsViewModel::setLanguage,
                    onLogout = authViewModel::logout,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("completed") {
                CompletedNotesScreen(
                    uiState = notesViewModel.uiState,
                    uiMessage = notesViewModel.uiMessage,
                    onRetryNotes = notesViewModel::loadNotes,
                    onMessageShown = notesViewModel::onMessageShown,
                    onMessageAction = onMessageAction,
                    onaddNoteClick = {
                        navController.navigate("edit")
                    },
                    onNoteClick = { noteId ->
                        navController.navigate("edit/${Uri.encode(noteId)}")
                    },
                    onCompletionChange = { noteId, isCompleted ->
                        notesViewModel.setNoteCompletion(noteId, isCompleted)
                    },
                    onNavigateHome = {
                        navController.popBackStack()
                    },
                    onUpcomingClick = {
                        navController.navigate("upcoming") { launchSingleTop = true }
                    }
                )
            }

            composable("upcoming") {
                UpcomingTasksScreen(
                    uiState = notesViewModel.uiState,
                    onRetryNotes = notesViewModel::loadNotes,
                    onAddNoteClick = { navController.navigate("edit") },
                    onNoteClick = { noteId ->
                        navController.navigate("edit/${Uri.encode(noteId)}")
                    },
                    onCompletedNotesClick = {
                        navController.navigate("completed") { launchSingleTop = true }
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
                    uiState = homeInfoViewModel.uiState,
                    uiMessage = homeInfoViewModel.uiMessage,
                    onRetry = homeInfoViewModel::loadCards,
                    onMessageShown = homeInfoViewModel::onMessageShown,
                    onAddClick = { navController.navigate("home_info_edit") },
                    onCardClick = { cardId ->
                        navController.navigate("home_info_edit/$cardId")
                    },
                    onLinkClick = { link ->
                        runCatching {
                            context.startActivity(homeInfoLinkIntent(link))
                        }
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
                val cards = (homeInfoViewModel.uiState as? HomeInfoUiState.Content)?.cards.orEmpty()
                val card = cards.find { it.id == cardId }
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
                    },
                    onBack = { navController.popBackStack() },
                    showFormHint = !onboardingViewModel.uiState.noteFormHintSeen,
                    onFormHintSeen = onboardingViewModel::markNoteFormHintSeen,
                    onVoiceInputUsed = achievementsViewModel::reportVoiceInputUsed
                )
            }

            composable(
                route = "edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments!!.getString("noteId") ?: ""
                val notes = (notesViewModel.uiState as? NotesUiState.Content)?.notes.orEmpty()
                val note = notes.find { it.id == noteId }
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
                    },
                    onBack = { navController.popBackStack() },
                    onVoiceInputUsed = achievementsViewModel::reportVoiceInputUsed
                )
            }
            }
            SyncStatusBanner(
                status = syncStatus,
                onRetry = onRetrySync,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            // WoW-плашка достижения поверх всех роутов; показываем только в сессии,
            // чтобы очередь анлоков не всплывала над экраном входа после выхода.
            if (authState is AuthState.Authenticated) {
                AchievementUnlockOverlay(
                    toast = achievementsViewModel.pendingToast,
                    onDismissed = achievementsViewModel::onToastDismissed,
                    onTapped = { toast ->
                        achievementsViewModel.onToastTapped(toast)
                        navController.navigate("achievements") { launchSingleTop = true }
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/**
 * Компактная плашка статуса синхронизации: небольшая «пилюля» сверху по центру
 * с кнопкой закрытия. Появляется только для залежавшихся операций (см.
 * [WorkManagerSyncCoordinator.refreshStatus]); закрытая плашка не показывается
 * снова, пока статус не изменится.
 */
@Composable
private fun SyncStatusBanner(
    status: SyncStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissedStatus by remember { mutableStateOf<SyncStatus?>(null) }
    if (status == SyncStatus.Idle || status == dismissedStatus) return
    val text = when (status) {
        SyncStatus.Idle -> return
        SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
        is SyncStatus.Pending -> stringResource(R.string.sync_status_pending, status.count)
        is SyncStatus.Offline -> stringResource(R.string.sync_status_offline, status.count)
        is SyncStatus.Blocked -> stringResource(R.string.sync_status_blocked)
    }
    Surface(
        modifier = modifier.padding(top = 4.dp),
        color = MaterialTheme.colors.surface,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, style = MaterialTheme.typography.caption)
            if (status != SyncStatus.Syncing) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.sync_retry), style = MaterialTheme.typography.caption)
                }
            }
            IconButton(onClick = { dismissedStatus = status }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

internal sealed interface PendingNotificationNavigationDecision {
    data object Wait : PendingNotificationNavigationDecision
    data class ClearPending(val noteId: String) : PendingNotificationNavigationDecision
    data class OpenNote(val noteId: String) : PendingNotificationNavigationDecision
}

internal fun pendingNotificationNavigationDecision(
    authState: AuthState,
    notesUiState: NotesUiState,
    pendingNotificationNoteId: String?,
    isWaitingForNotificationNotes: Boolean
): PendingNotificationNavigationDecision {
    val targetNoteId = pendingNotificationNoteId
        ?.takeIf { it.isNotBlank() }
        ?: return PendingNotificationNavigationDecision.Wait
    if (!isWaitingForNotificationNotes || authState !is AuthState.Authenticated) {
        return PendingNotificationNavigationDecision.Wait
    }

    return when (notesUiState) {
        is NotesUiState.Content -> {
            if (notesUiState.notes.any { it.id == targetNoteId }) {
                PendingNotificationNavigationDecision.OpenNote(targetNoteId)
            } else {
                PendingNotificationNavigationDecision.ClearPending(targetNoteId)
            }
        }
        NotesUiState.Empty -> PendingNotificationNavigationDecision.ClearPending(targetNoteId)
        is NotesUiState.Error,
        NotesUiState.Loading -> PendingNotificationNavigationDecision.Wait
    }
}
