package app.rotatescreen.domain.model

/**
 * Represents the current state of orientation control using FP immutable data
 */
data class OrientationState(
    val globalOrientation: ScreenOrientation = ScreenOrientation.Unspecified,
    val currentApp: String? = null,
    val perAppSettings: Map<String, List<AppOrientationSetting>> = emptyMap(),
    val isAccessibilityServiceEnabled: Boolean = false,
    val hasDrawOverlayPermission: Boolean = false
) {
    fun withGlobalOrientation(orientation: ScreenOrientation): OrientationState =
        copy(globalOrientation = orientation)

    fun withCurrentApp(packageName: String?): OrientationState =
        copy(currentApp = packageName)

    fun withPerAppSetting(setting: AppOrientationSetting): OrientationState {
        val currentList = perAppSettings[setting.packageName] ?: emptyList()
        // Replace setting for this specific screen, or add if new
        val updatedList = currentList.filter { it.targetScreen.id != setting.targetScreen.id } + setting
        return copy(perAppSettings = perAppSettings + (setting.packageName to updatedList))
    }

    fun removePerAppSetting(packageName: String): OrientationState =
        copy(perAppSettings = perAppSettings - packageName)

    fun removePerAppSettingForDisplay(packageName: String, displayId: Int): OrientationState {
        val currentList = perAppSettings[packageName] ?: return this
        val updatedList = currentList.filter { it.targetScreen.id != displayId }
        return if (updatedList.isEmpty()) {
            copy(perAppSettings = perAppSettings - packageName)
        } else {
            copy(perAppSettings = perAppSettings + (packageName to updatedList))
        }
    }

    fun withAccessibilityServiceEnabled(enabled: Boolean): OrientationState =
        copy(isAccessibilityServiceEnabled = enabled)

    fun withDrawOverlayPermission(granted: Boolean): OrientationState =
        copy(hasDrawOverlayPermission = granted)

    /**
     * Gets all settings for a specific app
     */
    fun getSettingsForApp(packageName: String): List<AppOrientationSetting> =
        perAppSettings[packageName] ?: emptyList()

    /**
     * Gets the setting for a specific app on a specific display
     */
    fun getSettingForAppAndDisplay(packageName: String, displayId: Int): AppOrientationSetting? =
        perAppSettings[packageName]?.firstOrNull { it.targetScreen.id == displayId }

    /**
     * Gets the effective orientation for the current or specified app
     * Returns the first enabled setting, or global orientation if none found
     */
    fun getEffectiveOrientation(packageName: String? = currentApp): ScreenOrientation =
        packageName
            ?.let { perAppSettings[it] }
            ?.firstOrNull { it.enabled }
            ?.orientation
            ?: globalOrientation

    fun isFullyConfigured(): Boolean =
        isAccessibilityServiceEnabled && hasDrawOverlayPermission
}
