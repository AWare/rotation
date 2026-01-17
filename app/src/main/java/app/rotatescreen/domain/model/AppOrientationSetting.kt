package app.rotatescreen.domain.model

/**
 * Represents orientation setting for a specific app using FP style
 */
data class AppOrientationSetting(
    val packageName: String,
    val appName: String,
    val orientation: ScreenOrientation,
    val targetScreen: TargetScreen,
    val enabled: Boolean = true
) {
    companion object {
        fun create(
            packageName: String,
            appName: String,
            orientation: ScreenOrientation = ScreenOrientation.Unspecified,
            targetScreen: TargetScreen = TargetScreen.AllScreens,
            enabled: Boolean = true
        ): AppOrientationSetting =
            AppOrientationSetting(packageName, appName, orientation, targetScreen, enabled)
    }

    fun withOrientation(newOrientation: ScreenOrientation): AppOrientationSetting =
        copy(orientation = newOrientation)

    fun withTargetScreen(newScreen: TargetScreen): AppOrientationSetting =
        copy(targetScreen = newScreen)

    fun toggleEnabled(): AppOrientationSetting =
        copy(enabled = !enabled)
}

/**
 * Represents an installed app that can have orientation settings
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val iconPath: String? = null,
    val isRecent: Boolean = false
)
