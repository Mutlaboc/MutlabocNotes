package com.example.mutlabocsnotes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.mutlabocsnotes.SessionManager

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        setContent {
            MyApp()
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
    notesViewModel: NotesViewModel = viewModel(),
    homeInfoViewModel: HomeInfoViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val startDestination = remember {
        if (sessionManager.hasSession()|| FirebaseAuth.getInstance().currentUser != null) "home" else "auth"
    }

    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(colors = if (isDarkTheme) darkColors() else lightColors()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            composable("auth") {
                AuthScreen {
                    notesViewModel.loadNotes()
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }

            composable("home") {
                HomeScreen(
                    notes = notesViewModel.notes,
                    totalCoins = notesViewModel.totalCoins,
                    userEmail = sessionManager.getEmail().orEmpty().ifBlank {
                        FirebaseAuth.getInstance().currentUser?.email ?: ""
                    },
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
                    onSwitchUser = {
                        sessionManager.clear()
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("auth") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDarkTheme = it },
                    onDeleteAccount = {
                        sessionManager.clear()
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            user.delete().addOnCompleteListener {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("auth") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        } else {
                            navController.navigate("auth") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("completed") {
                CompletedNotesScreen(
                    notes = notesViewModel.notes,
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
