package app.rotatescreen.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Aspect ratio of a screen
 */
enum class AspectRatio {
    PORTRAIT,    // Taller than wide
    LANDSCAPE,   // Wider than tall
    SQUARE       // Approximately equal
}

/**
 * Represents target display/screen for orientation control
 */
sealed class TargetScreen(val id: Int, val displayName: String, val aspectRatio: AspectRatio = AspectRatio.LANDSCAPE) {
    data object AllScreens : TargetScreen(-1, "All Screens", AspectRatio.SQUARE)
    data class SpecificScreen(
        val displayId: Int,
        val name: String,
        val ratio: AspectRatio = AspectRatio.LANDSCAPE
    ) : TargetScreen(displayId, name, ratio) {
        companion object {
            fun primary(aspectRatio: AspectRatio = AspectRatio.LANDSCAPE): SpecificScreen =
                SpecificScreen(0, "Primary Screen", aspectRatio)
            fun secondary(displayId: Int, aspectRatio: AspectRatio = AspectRatio.LANDSCAPE): SpecificScreen =
                SpecificScreen(displayId, "Secondary Screen $displayId", aspectRatio)
        }
    }

    companion object {
        fun fromId(id: Int, name: String = "", aspectRatio: AspectRatio = AspectRatio.LANDSCAPE): Either<ScreenError, TargetScreen> =
            when {
                id == -1 -> AllScreens.right()
                id >= 0 -> SpecificScreen(id, name.ifEmpty { "Screen $id" }, aspectRatio).right()
                else -> ScreenError.InvalidDisplayId(id).left()
            }
    }
}

sealed class ScreenError {
    data class InvalidDisplayId(val id: Int) : ScreenError()
    data class DisplayNotFound(val id: Int) : ScreenError()
}
