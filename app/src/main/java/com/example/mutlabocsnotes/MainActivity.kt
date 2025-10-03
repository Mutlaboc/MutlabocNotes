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

class MainActivity : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       FirebaseApp.initializeApp(this)
       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
       setContent {
           MyApp()

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
                AuthScreeen {
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
                userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "",
                onAddNoteClick = {
                    navController.navigate("edit")
                },
                onNoteClick = { noteId ->
                    navController.navigate("edit/$noteId")
                },
                onOtherCellClick = { _ -> },
                onSwitchUser = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo("home") {inclusive = true}
                    }
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



