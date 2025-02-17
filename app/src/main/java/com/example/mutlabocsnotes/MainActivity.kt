package com.example.mutlabocsnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContent {
           MyApp()
       }
   }
}
@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home") {
        composable("home") {
            HomeScreen(
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
                onSaveClick = {
                    navController.popBackStack()
                }
            )
        }

    }
}