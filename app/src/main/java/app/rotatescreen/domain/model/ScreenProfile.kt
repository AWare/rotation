package app.rotatescreen.domain.model

/**
 * Represents a saved profile for multi-screen orientation setup
 */
data class ScreenProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val screenConfigurations: Map<Int, ScreenOrientation>, // displayId -> orientation
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(
            name: String,
            description: String = "",
            screenConfigurations: Map<Int, ScreenOrientation> = emptyMap()
        ): ScreenProfile = ScreenProfile(
            name = name,
            description = description,
            screenConfigurations = screenConfigurations
        )

        // Predefined profiles
        fun deskSetup(): ScreenProfile = ScreenProfile(
            id = "desk_setup",
            name = "Desk Setup",
            description = "Laptop portrait + external monitor landscape",
            screenConfigurations = mapOf(
                0 to ScreenOrientation.Portrait,
                1 to ScreenOrientation.Landscape
            )
        )

        fun presentation(): ScreenProfile = ScreenProfile(
            id = "presentation",
            name = "Presentation",
            description = "Phone locked portrait + TV landscape",
            screenConfigurations = mapOf(
                0 to ScreenOrientation.Portrait,
                1 to ScreenOrientation.Landscape
            )
        )

        fun allAutoRotate(): ScreenProfile = ScreenProfile(
            id = "all_auto",
            name = "All Auto-Rotate",
            description = "All screens in sensor mode",
            screenConfigurations = emptyMap() // Empty means use Sensor for all
        )
    }

    fun withActive(active: Boolean): ScreenProfile = copy(isActive = active)

    fun withConfiguration(displayId: Int, orientation: ScreenOrientation): ScreenProfile =
        copy(screenConfigurations = screenConfigurations + (displayId to orientation))

    fun removeDisplay(displayId: Int): ScreenProfile =
        copy(screenConfigurations = screenConfigurations - displayId)
}
