package app.homenotes.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val HOME_SEARCH_FIELD_TEST_TAG = "home_search_field"
const val BOTTOM_BAR_TEST_TAG = "bottom_bar"
const val USER_MENU_BUTTON_TEST_TAG = "user_menu_button"
const val USER_MENU_SETTINGS_ITEM_TEST_TAG = "user_menu_settings_item"

fun noteItemTestTag(noteId: String): String = "note_item_$noteId"

enum class BottomBarAction {
    CompletedNotes,
    AddNote,
    HomeInfo,
    UpcomingTasks
}

@Composable
fun HomeScreen(
    uiState: NotesUiState,
    uiMessage: UiMessage?,
    userEmail: String,
    onRetryNotes: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onMessageAction: (UiMessageAction) -> Unit,
    onAddNoteClick: () -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onCompletedNotesClick: () -> Unit,
    onHomeInfoClick: () -> Unit,
    onCompletionChange: (noteId: String, Boolean) -> Unit,
    onChecklistItemToggle: (noteId: String, index: Int, checked: Boolean) -> Unit = { _, _, _ -> },
    // Id of a just-created note: its NoteItem plays a one-shot entrance animation and
    // reports back via [onNewNoteShown] so the flag is cleared after the first show.
    newlyCreatedNoteId: String? = null,
    onNewNoteShown: () -> Unit = {},
    onSwitchUser: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCharacter: () -> Unit = {},
    characterSheet: CharacterSheet? = null,
    // Полоса событий фокус-таймера: записи текущей сессии и колбэки её жизненного цикла.
    focusEventsFeed: List<FocusFeedEntry> = emptyList(),
    onFocusSessionStart: () -> Unit = {},
    onFocusMinuteTick: (skillKey: String?) -> Unit = {},
    onboarding: OnboardingState = OnboardingState.Completed,
    onWelcomeSeen: () -> Unit = {},
    onHintSeen: (OnboardingHintStep) -> Unit = {},
    earnedCoins: Int
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    // Intercept the system back press on the notes screen to confirm leaving the app.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    if (showExitDialog) {
        ExitAppDialog(
            onConfirm = {
                showExitDialog = false
                context.findActivity()?.finish()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    val scaffoldState = rememberScaffoldState()
    val snackbarText = uiMessage?.text?.asString()
    val snackbarActionText = uiMessage?.actionText?.asString()
    val contentState = uiState as? NotesUiState.Content
    val notes = contentState?.notes.orEmpty()
    val totalCoins = contentState?.totalCoins ?: 0
    val nowMillis = rememberTaskClock(notes)
    val activeNotes = homeNotes(notes, nowMillis)
    val homeAnimationRestartKey = remember(uiState, activeNotes, totalCoins) {
        homeAnimationRestartKey(uiState, activeNotes, totalCoins)
    }
    var search by remember { mutableStateOf("") }

    // --- Онбординг: приветствие и одноразовые подсказки над иконками нижней панели ---
    val iconBounds = remember { mutableStateMapOf<BottomBarAction, Rect>() }
    val activeHint: OnboardingHintStep? = when {
        !onboarding.welcomeSeen -> null
        !onboarding.addNoteHintSeen -> OnboardingHintStep.ADD_NOTE
        !onboarding.homeInfoHintSeen -> OnboardingHintStep.HOME_INFO
        !onboarding.completedHintSeen -> OnboardingHintStep.COMPLETED
        else -> null
    }
    val highlightedAction = activeHint?.let(::hintBottomBarAction)

    // Coin-flight animation state: the header counter's anchor, the active flights, and an
    // id source. Completing a note is deferred until its coins reach the counter.
    var coinTarget by remember { mutableStateOf(Offset.Zero) }
    val flights = remember { mutableStateListOf<CoinFlight>() }
    val bursts = remember { mutableStateListOf<NoteBurst>() }
    var nextFlightId by remember { mutableStateOf(0L) }

    // Per-note stopwatch state (in memory only — not persisted). [timerAccum] holds the
    // paused value; while a note runs we add (now - runStart) on top.
    val timerAccum = remember { mutableStateMapOf<String, Long>() }
    var runningNoteId by remember { mutableStateOf<String?>(null) }
    var expandedNoteId by remember { mutableStateOf<String?>(null) }
    var runStartUptime by remember { mutableStateOf(0L) }
    var timerOriginRect by remember { mutableStateOf(Rect.Zero) }
    // Random skill (from the character's skills) credited during this timer session.
    var timerSkillKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiMessage?.id) {
        val message = uiMessage ?: return@LaunchedEffect
        val text = snackbarText ?: return@LaunchedEffect
        val result = scaffoldState.snackbarHostState.showSnackbar(
            message = text,
            actionLabel = snackbarActionText
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.action?.let(onMessageAction)
        }
        onMessageShown(message.id)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = CozyAuth.Cream,
        bottomBar = {
            BottomBar(
                selectedAction = null,
                onCompletedNotesClick = onCompletedNotesClick,
                onAddClick = onAddNoteClick,
                onHomeInfoClick = onHomeInfoClick,
                highlightedAction = highlightedAction,
                onIconBounds = { action, rect -> iconBounds[action] = rect }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            when (earnedCoins){
                in 0..99 -> HomeHeader(
                    totalCoins = totalCoins,
                    animationRestartKey = homeAnimationRestartKey,
                    onCoinAnchorPositioned = { coinTarget = it },
                    onOpenCharacter = onOpenCharacter,
                    stage = 0
                )
                in 100..200 -> HomeHeader(
                    totalCoins = totalCoins,
                    animationRestartKey = homeAnimationRestartKey,
                    onCoinAnchorPositioned = { coinTarget = it },
                    onOpenCharacter = onOpenCharacter,
                    stage = 1
                    )
                in 200..300 -> HomeHeader(
                    totalCoins = totalCoins,
                    animationRestartKey = homeAnimationRestartKey,
                    onCoinAnchorPositioned = { coinTarget = it },
                    onOpenCharacter = onOpenCharacter,
                    stage = 2
                )
                 in 300..Int.MAX_VALUE -> HomeHeader(
                    totalCoins = totalCoins,
                    animationRestartKey = homeAnimationRestartKey,
                    onCoinAnchorPositioned = { coinTarget = it },
                    onOpenCharacter = onOpenCharacter,
                    stage = 3
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CozyTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = stringResource(R.string.search_label),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                        .testTag(HOME_SEARCH_FIELD_TEST_TAG)
                )
                UserMenu(
                    userEmail = userEmail,
                    onSwitchUser = onSwitchUser,
                    onOpenSettings = onOpenSettings
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState) {
                    is NotesUiState.Error -> item {
                        NotesStatusMessage(
                            message = uiState.message.asString(),
                            actionText = stringResource(R.string.action_retry),
                            onAction = onRetryNotes
                        )
                    }

                    NotesUiState.Loading -> item {
                        NotesLoadingMessage()
                    }

                    NotesUiState.Empty -> item {
                        NotesStatusMessage(message = stringResource(R.string.notes_empty))
                    }

                    is NotesUiState.Content -> {
                        val query = search.trim()
                        val visibleNotes = activeNotes.filter { note ->
                            query.isBlank() ||
                                note.title.contains(query, ignoreCase = true) ||
                                note.content.contains(query, ignoreCase = true)
                        }
                        if (visibleNotes.isEmpty()) {
                            item {
                                NotesStatusMessage(message = stringResource(R.string.notes_empty))
                            }
                        }
                        items(visibleNotes, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                modifier = Modifier.testTag(noteItemTestTag(note.id)),
                                onCompletionChange = { isCompleted ->
                                    onCompletionChange(note.id, isCompleted)
                                },
                                animateEntrance = note.id == newlyCreatedNoteId,
                                onEntranceShown = onNewNoteShown,
                                onBurstComplete = { burstCenter, coinSource, coinCount, color ->
                                    nextFlightId += 1
                                    bursts.add(
                                        NoteBurst(
                                            id = nextFlightId,
                                            centerRoot = burstCenter,
                                            color = color
                                        )
                                    )
                                    flights.add(
                                        CoinFlight(
                                            id = nextFlightId,
                                            noteId = note.id,
                                            startRoot = coinSource,
                                            count = coinCount
                                        )
                                    )
                                },
                                timerMillis = timerAccum[note.id] ?: 0L,
                                isExpanded = expandedNoteId == note.id,
                                onStartTimer = { rect ->
                                    if (expandedNoteId == null) {
                                        timerOriginRect = rect
                                        runStartUptime = SystemClock.uptimeMillis()
                                        timerSkillKey = characterSheet?.skills?.randomOrNull()?.key
                                        runningNoteId = note.id
                                        expandedNoteId = note.id
                                        onFocusSessionStart()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

            CoinFlightOverlay(
                flights = flights,
                bursts = bursts,
                targetRoot = coinTarget,
                onFlightArrived = { flight ->
                    flights.remove(flight)
                    onCompletionChange(flight.noteId, true)
                },
                onBurstFinished = { burst -> bursts.remove(burst) }
            )

            val expandedNote = expandedNoteId?.let { id -> notes.find { it.id == id } }
            if (expandedNote != null) {
                // Commits the running segment to the accumulator. XP за время больше не
                // начисляется — награды приходят только из случайных событий полосы.
                val settleRun: () -> Unit = settle@{
                    if (runningNoteId != expandedNote.id) return@settle
                    val segment = (SystemClock.uptimeMillis() - runStartUptime).coerceAtLeast(0L)
                    val base = timerAccum[expandedNote.id] ?: 0L
                    timerAccum[expandedNote.id] = base + segment
                    runningNoteId = null
                }
                ExpandedNoteOverlay(
                    note = expandedNote,
                    accumulatedMs = timerAccum[expandedNote.id] ?: 0L,
                    running = runningNoteId == expandedNote.id,
                    runStartUptime = runStartUptime,
                    onPause = settleRun,
                    onResume = {
                        runStartUptime = SystemClock.uptimeMillis()
                        runningNoteId = expandedNote.id
                    },
                    onComplete = {
                        settleRun()
                        onCompletionChange(expandedNote.id, true)
                    },
                    onReturnHome = settleRun,
                    onClosed = { expandedNoteId = null },
                    focusEvents = focusEventsFeed,
                    onFocusMinuteTick = { onFocusMinuteTick(timerSkillKey) },
                    onChecklistItemToggle = { index, checked ->
                        onChecklistItemToggle(expandedNote.id, index, checked)
                    }
                )
            }

            // Приветствие при первом запуске приложения.
            if (!onboarding.welcomeSeen) {
                OnboardingWelcomeDialog(
                    title = stringResource(R.string.onboarding_welcome_title),
                    message = stringResource(R.string.onboarding_welcome_message),
                    buttonText = stringResource(R.string.onboarding_welcome_button),
                    onDismiss = onWelcomeSeen
                )
            }

            // Одноразовая подсказка над подсвеченной иконкой нижней панели.
            val hintAnchor = highlightedAction?.let { iconBounds[it] }
            if (activeHint != null && hintAnchor != null && hintAnchor != Rect.Zero) {
                OnboardingCoachOverlay(
                    anchorRect = hintAnchor,
                    title = stringResource(onboardingHintTitleRes(activeHint)),
                    message = stringResource(onboardingHintMessageRes(activeHint)),
                    buttonText = stringResource(onboardingHintButtonRes(activeHint)),
                    onDismiss = { onHintSeen(activeHint) }
                )
            }
        }
    }
}

private fun hintBottomBarAction(step: OnboardingHintStep): BottomBarAction = when (step) {
    OnboardingHintStep.ADD_NOTE -> BottomBarAction.AddNote
    OnboardingHintStep.HOME_INFO -> BottomBarAction.HomeInfo
    OnboardingHintStep.COMPLETED -> BottomBarAction.CompletedNotes
}

private fun onboardingHintTitleRes(step: OnboardingHintStep): Int = when (step) {
    OnboardingHintStep.ADD_NOTE -> R.string.onboarding_hint_add_note_title
    OnboardingHintStep.HOME_INFO -> R.string.onboarding_hint_home_info_title
    OnboardingHintStep.COMPLETED -> R.string.onboarding_hint_completed_title
}

private fun onboardingHintMessageRes(step: OnboardingHintStep): Int = when (step) {
    OnboardingHintStep.ADD_NOTE -> R.string.onboarding_hint_add_note_message
    OnboardingHintStep.HOME_INFO -> R.string.onboarding_hint_home_info_message
    OnboardingHintStep.COMPLETED -> R.string.onboarding_hint_completed_message
}

private fun onboardingHintButtonRes(step: OnboardingHintStep): Int = when (step) {
    OnboardingHintStep.COMPLETED -> R.string.onboarding_done
    else -> R.string.onboarding_next
}

@Composable
private fun ExitAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Text(
                text = "Выход",
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont
            )
        },
        text = {
            Text(
                text = "Выйти из приложения?",
                color = CozyAuth.InkSoft,
                fontFamily = CozyAuth.PixelFont
            )
        },
        confirmButton = {
            PixelPrimaryButton(
                text = "Выйти",
                onClick = onConfirm
            )
        },
        dismissButton = {
            PixelOutlineButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss
            )
        }
    )
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun homeAnimationRestartKey(
    uiState: NotesUiState,
    activeNotes: List<Note>,
    totalCoins: Int
): String {
    return when (uiState) {
        NotesUiState.Empty -> "empty:$totalCoins"
        is NotesUiState.Error -> "error:${uiState.message}:$totalCoins"
        NotesUiState.Loading -> "loading:$totalCoins"
        is NotesUiState.Content -> {
            val activeNotesKey = activeNotes.joinToString(separator = "|") { note ->
                "${note.id}:${note.isCompleted}"
            }
            "content:$totalCoins:$activeNotesKey"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleNotes = listOf(
        Note(id = "1", title = "Note 1", content = "Note content"),
        Note(id = "2", title = "Note 2", content = "Note content", category = NoteCategory.SHOPPING),
        Note(id = "3", title = "Note 3", content = "Note content", category = NoteCategory.TASKS)
    )
    HomeScreen(
        uiState = NotesUiState.Content(sampleNotes, totalCoins = 12),
        uiMessage = null,
        userEmail = "user@example.com",
        onRetryNotes = {},
        onMessageShown = {},
        onMessageAction = {},
        onAddNoteClick = {},
        onNoteClick = {},
        onCompletedNotesClick = {},
        onHomeInfoClick = {},
        onCompletionChange = { _, _ -> },
        onSwitchUser = {},
        onOpenSettings = {},
        earnedCoins = 1
    )
}
