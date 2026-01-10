package com.example.counterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.counterapp.data.CounterDatabase
import com.example.counterapp.data.CounterRepository
import com.example.counterapp.ui.HomeViewModel
import com.example.counterapp.ui.HistoryViewModel
import com.example.counterapp.ui.screens.HomeScreen
import com.example.counterapp.ui.screens.HistoryScreen
import com.example.counterapp.ui.theme.CounterAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = CounterDatabase.getDatabase(applicationContext)
        val repository = CounterRepository(database.counterDao(), database.eventLogDao())

        setContent {
            CounterAppTheme {
                AppNavigation(repository)
            }
        }
    }
}

@Composable
fun AppNavigation(repository: CounterRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(repository) as T
                    }
                }
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToHistory = { id -> navController.navigate("history/$id") }
            )
        }
        composable(
            "history/{counterId}",
            arguments = listOf(navArgument("counterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val counterId = backStackEntry.arguments?.getLong("counterId") ?: 0L
            val viewModel: HistoryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HistoryViewModel(repository, counterId) as T
                    }
                }
            )
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
