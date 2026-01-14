package com.example.mutlabocsnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       FirebaseApp.initializeApp(this)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
fun MyApp(notesViewModel: NotesViewModel = viewModel()) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) "home" else "auth"
    }
    NavHost(
        navController = navController,
        startDestination = startDestination) {

        composable("auth") {
                AuthScreen {
                    notesViewModel.loadNotes()
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true
                        }

                    }
                }
            }
        composable("home") {
            HomeScreen(
                notes = notesViewModel.notes,
                totalCoins = notesViewModel.totalCoins,
                userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "",
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
                        2 -> Unit
                    }
                },
                onCompletionChange = { noteId, isCompleted ->
                    notesViewModel.setNoteCompletion(noteId, isCompleted)
                },
                onSwitchUser = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo("home") {inclusive = true}
                    }
                }
            )
        }
        composable("completed") {
            CompletedNotesScreen(
                notes = notesViewModel.notes,
                onaddNoteClick = {
                    navController.navigate("edit")
                },
                onNoteClick = {
                    noteId ->
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
        ) {backStackEntry ->
            val noteId = backStackEntry.arguments!!.getString("noteId") ?: ""
            val note = notesViewModel.notes.find { it.id == noteId }
            EditNoteScreen(
                note = note,
                onSaveClick = { updatedNote ->
                    if (note != null) {
                        notesViewModel.updateNote(updatedNote)
                    }
                    navController.popBackStack() },
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



