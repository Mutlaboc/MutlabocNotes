package com.example.mutlabocsnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
       setContent {
           MyApp()

       }
   }
}
@Composable
fun MyApp(notesViewModel: NotesViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home") {
        composable("home") {
            HomeScreen(
                notes = notesViewModel.notes,
                onAddNoteClick = {
                    navController.navigate("edit")
                },
                onNoteClick = { noteId ->
                    navController.navigate("edit/$noteId")
                }
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
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) {backStackEntry ->
            val noteId = backStackEntry.arguments!!.getInt("noteId")
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



