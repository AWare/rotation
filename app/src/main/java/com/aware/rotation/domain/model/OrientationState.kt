package com.aware.rotation.domain.model

/**
 * Represents the current state of orientation control using FP immutable data
 */
data class OrientationState(
    val globalOrientation: ScreenOrientation = ScreenOrientation.Unspecified,
    val currentApp: String? = null,
    val perAppSettings: Map<String, AppOrientationSetting> = emptyMap(),
    val isAccessibilityServiceEnabled: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false
) {
    fun withGlobalOrientation(orientation: ScreenOrientation): OrientationState =
        copy(globalOrientation = orientation)

    fun withCurrentApp(packageName: String?): OrientationState =
        copy(currentApp = packageName)

    fun withPerAppSetting(setting: AppOrientationSetting): OrientationState =
        copy(perAppSettings = perAppSettings + (setting.packageName to setting))

    fun removePerAppSetting(packageName: String): OrientationState =
        copy(perAppSettings = perAppSettings - packageName)

    fun withAccessibilityServiceEnabled(enabled: Boolean): OrientationState =
        copy(isAccessibilityServiceEnabled = enabled)

    fun withWriteSettingsPermission(granted: Boolean): OrientationState =
        copy(hasWriteSettingsPermission = granted)

    /**
     * Gets the effective orientation for the current or specified app
     */
    fun getEffectiveOrientation(packageName: String? = currentApp): ScreenOrientation =
        packageName
            ?.let { perAppSettings[it] }
            ?.takeIf { it.enabled }
            ?.orientation
            ?: globalOrientation

    fun isFullyConfigured(): Boolean =
        isAccessibilityServiceEnabled && hasWriteSettingsPermission
}
