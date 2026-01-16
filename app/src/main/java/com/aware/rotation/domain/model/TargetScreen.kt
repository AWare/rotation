package com.aware.rotation.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Represents target display/screen for orientation control
 */
sealed class TargetScreen(val id: Int, val displayName: String) {
    data object AllScreens : TargetScreen(-1, "All Screens")
    data class SpecificScreen(val displayId: Int, val name: String) : TargetScreen(displayId, name) {
        companion object {
            fun primary(): SpecificScreen = SpecificScreen(0, "Primary Screen")
            fun secondary(displayId: Int): SpecificScreen = SpecificScreen(displayId, "Secondary Screen $displayId")
        }
    }

    companion object {
        fun fromId(id: Int, name: String = ""): Either<ScreenError, TargetScreen> =
            when {
                id == -1 -> AllScreens.right()
                id >= 0 -> SpecificScreen(id, name.ifEmpty { "Screen $id" }).right()
                else -> ScreenError.InvalidDisplayId(id).left()
            }
    }
}

sealed class ScreenError {
    data class InvalidDisplayId(val id: Int) : ScreenError()
    data class DisplayNotFound(val id: Int) : ScreenError()
}
