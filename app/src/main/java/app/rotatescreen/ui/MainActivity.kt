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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.rotatescreen.ui.components.RiscOsColors
import app.rotatescreen.ui.navigation.Screen
import app.rotatescreen.ui.screen.AppConfigScreen
import app.rotatescreen.ui.screen.LogViewerScreen
import app.rotatescreen.ui.screen.MainScreen
import app.rotatescreen.ui.screen.MultiScreenManagerScreen
import app.rotatescreen.ui.screen.PerAppSettingsScreen
import app.rotatescreen.ui.screen.PermissionCheckScreen
import app.rotatescreen.ui.theme.RotationTheme
import app.rotatescreen.util.ComprehensivePermissionChecker

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

        // Get target screen from intent
        val targetScreen = intent?.getStringExtra(EXTRA_SCREEN)

        viewModel.observePalette()

        setContent {
            RotationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RotationNavHost(
                        viewModel = viewModel,
                        initialPackage = targetPackage,
                        initialScreen = targetScreen
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
                viewModel.cyclePalette()
                true
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                // L1 button - previous palette
                viewModel.cyclePaletteBack()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_SCREEN = "screen"
    }
}

@Composable
fun RotationNavHost(
    viewModel: MainViewModel,
    initialPackage: String? = null,
    initialScreen: String? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permissions on startup and when resuming
    var showPermissionCheck by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableStateOf(false) }

    // Check permissions on first composition and when returning from settings
    LaunchedEffect(Unit) {
        val hasMissing = ComprehensivePermissionChecker.hasMissingCriticalPermissions(context)
        showPermissionCheck = hasMissing
        permissionsChecked = true
    }

    // Re-check permissions when activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasMissing = ComprehensivePermissionChecker.hasMissingCriticalPermissions(context)
                showPermissionCheck = hasMissing
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Determine start destination based on initial parameters
    val startDestination = when {
        initialPackage != null -> Screen.AppConfig.createRoute(initialPackage)
        initialScreen == "multi_screen_manager" -> Screen.MultiScreenManager.route
        else -> Screen.Global.route
    }

    // Show permission check overlay if permissions are missing
    if (showPermissionCheck) {
        PermissionCheckScreen(
            onDismiss = {
                showPermissionCheck = false
            },
            onPermissionsGranted = {
                showPermissionCheck = false
            }
        )
    } else if (permissionsChecked) {
        // Only show main content after permissions are checked
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
                },
                onNavigateToMultiScreenManager = {
                    navController.navigate(Screen.MultiScreenManager.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.LogViewer.route)
                }
            )
        }

        composable(Screen.LogViewer.route) {
            LogViewerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MultiScreenManager.route) {
            MultiScreenManagerScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
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
}
