package app.rotatescreen.domain.model

/**
 * Represents the status of all required permissions
 */
data class PermissionStatus(
    val hasWriteSettings: Boolean,
    val hasOverlayPermission: Boolean,
    val hasUsageStatsPermission: Boolean,
    val isAccessibilityServiceEnabled: Boolean,
    val tilesAdded: TileStatus
) {
    /**
     * Check if all critical permissions are granted
     */
    fun allGranted(): Boolean =
        hasWriteSettings &&
        hasOverlayPermission &&
        hasUsageStatsPermission &&
        isAccessibilityServiceEnabled

    /**
     * Check if all permissions including tiles are granted
     */
    fun allIncludingTilesGranted(): Boolean =
        allGranted() && tilesAdded.allTilesAdded()

    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(): List<String> = buildList {
        if (!hasWriteSettings) add("System Settings")
        if (!hasOverlayPermission) add("Draw Over Apps")
        if (!hasUsageStatsPermission) add("Usage Stats")
        if (!isAccessibilityServiceEnabled) add("Accessibility Service")
    }

    /**
     * Get list of missing tiles
     */
    fun getMissingTiles(): List<String> = buildList {
        if (!tilesAdded.orientationTileAdded) add("Screen Rotation Tile")
        if (!tilesAdded.globalOrientationTileAdded) add("Global Rotation Tile")
        if (!tilesAdded.currentAppTileAdded) add("Current App Tile")
    }
}

/**
 * Status of Quick Settings tiles
 */
data class TileStatus(
    val orientationTileAdded: Boolean,
    val globalOrientationTileAdded: Boolean,
    val currentAppTileAdded: Boolean
) {
    fun allTilesAdded(): Boolean =
        orientationTileAdded && globalOrientationTileAdded && currentAppTileAdded

    fun anyTileAdded(): Boolean =
        orientationTileAdded || globalOrientationTileAdded || currentAppTileAdded

    fun tilesAddedCount(): Int =
        listOf(orientationTileAdded, globalOrientationTileAdded, currentAppTileAdded).count { it }

    companion object {
        val NONE = TileStatus(
            orientationTileAdded = false,
            globalOrientationTileAdded = false,
            currentAppTileAdded = false
        )

        val ALL = TileStatus(
            orientationTileAdded = true,
            globalOrientationTileAdded = true,
            currentAppTileAdded = true
        )
    }
}
