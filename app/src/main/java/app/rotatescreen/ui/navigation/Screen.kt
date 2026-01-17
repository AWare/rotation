package app.rotatescreen.ui.navigation

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    object Global : Screen("global")
    object PerApp : Screen("per_app")
}
