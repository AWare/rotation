package app.rotatescreen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.rotatescreen.ui.navigation.Screen
import app.rotatescreen.ui.screen.MainScreen
import app.rotatescreen.ui.screen.PerAppSettingsScreen
import app.rotatescreen.ui.theme.RotationTheme

/**
 * Main activity using Jetpack Compose with navigation
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RotationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RotationNavHost(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions when returning to the app
        viewModel.checkPermissions()
    }
}

@Composable
fun RotationNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Global.route
    ) {
        composable(Screen.Global.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToPerApp = {
                    navController.navigate(Screen.PerApp.route)
                }
            )
        }

        composable(Screen.PerApp.route) {
            PerAppSettingsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
