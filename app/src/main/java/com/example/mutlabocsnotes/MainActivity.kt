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
                onAddNoteClick = {
                    navController.navigate("edit")
                },
                onNoteClick = { noteId ->
                    navController.navigate("edit/$noteId")
                },
                onOtherCellClick = { _ -> }
            )
        }
        composable("edit") {
            EditNoteScreen(
                note = null,
                onSaveClick = { title, content ->
                    notesViewModel.addNote(title, content)
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
                onSaveClick = { title, content ->
                    if (note != null) {
                        notesViewModel.updateNote(noteId, title, content)
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



