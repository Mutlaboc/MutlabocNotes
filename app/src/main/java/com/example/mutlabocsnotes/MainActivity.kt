package com.example.mutlabocsnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
        composable(
            route = "edit",
        ) {
            EditNoteScreen(
                note = null,
                onSaveClick = { title, content ->
                    notesViewModel.addNote(title, content)
                    navController.popBackStack()
                }
            )
        }

    }
}

