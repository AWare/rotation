package app.rotatescreen.domain.model

import android.content.pm.ActivityInfo
import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Represents screen orientation options using FP style
 */
sealed class ScreenOrientation(val value: Int, val displayName: String) {
    data object Unspecified : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, "Auto Rotate")
    data object Portrait : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "Portrait")
    data object Landscape : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "Landscape")
    data object ReversePortrait : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, "Reverse Portrait")
    data object ReverseLandscape : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, "Reverse Landscape")
    data object Sensor : ScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR, "Sensor")

    companion object {
        fun fromValue(value: Int): Either<OrientationError, ScreenOrientation> =
            when (value) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> Unspecified.right()
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> Portrait.right()
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> Landscape.right()
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT -> ReversePortrait.right()
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> ReverseLandscape.right()
                ActivityInfo.SCREEN_ORIENTATION_SENSOR -> Sensor.right()
                else -> OrientationError.InvalidOrientation(value).left()
            }

        fun all(): List<ScreenOrientation> = listOf(
            Unspecified,
            Portrait,
            Landscape,
            ReversePortrait,
            ReverseLandscape,
            Sensor
        )
    }

    fun next(): ScreenOrientation = when (this) {
        Unspecified -> Portrait
        Portrait -> Landscape
        Landscape -> ReversePortrait
        ReversePortrait -> ReverseLandscape
        ReverseLandscape -> Sensor
        Sensor -> Unspecified
    }
}

sealed class OrientationError {
    data class InvalidOrientation(val value: Int) : OrientationError()
    data class PermissionDenied(val permission: String) : OrientationError()
    data class ServiceNotRunning(val serviceName: String) : OrientationError()
    data class DatabaseError(val message: String) : OrientationError()
}
