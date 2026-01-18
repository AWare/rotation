package app.rotatescreen.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.rotatescreen.ui.components.RiscOsColors
import app.rotatescreen.ui.navigation.Screen
import app.rotatescreen.ui.screen.AppConfigScreen
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

        // Get package name from intent if launched from tile, with validation
        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
            ?.takeIf { pkg ->
                pkg.isNotBlank() &&
                pkg.length <= 255 &&
                pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
            }

        setContent {
            RotationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RotationNavHost(
                        viewModel = viewModel,
                        initialPackage = targetPackage
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions when returning to the app
        viewModel.checkPermissions()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                // R1 button - next palette
                RiscOsColors.nextPalette()
                true
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                // L1 button - previous palette
                RiscOsColors.previousPalette()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}

@Composable
fun RotationNavHost(
    viewModel: MainViewModel,
    initialPackage: String? = null
) {
    val navController = rememberNavController()

    // Determine start destination based on initial package
    val startDestination = if (initialPackage != null) {
        Screen.AppConfig.createRoute(initialPackage)
    } else {
        Screen.Global.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Global.route) {
            // Handle back press on main screen to exit app
            BackHandler {
                // Do nothing - prevents navigating back from main screen
                // User must use home button to exit
            }

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
                },
                onAppClick = { packageName ->
                    navController.navigate(Screen.AppConfig.createRoute(packageName))
                }
            )
        }

        composable(
            route = Screen.AppConfig.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            AppConfigScreen(
                packageName = packageName,
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
