package app.rotatescreen.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import app.rotatescreen.domain.model.OrientationError

/**
 * Utility for checking permissions using FP style
 */
object PermissionChecker {
    fun hasWriteSettingsPermission(context: Context): Either<OrientationError, Boolean> =
        Either.catch {
            Settings.System.canWrite(context)
        }.mapLeft {
            OrientationError.PermissionDenied("WRITE_SETTINGS")
        }

    fun checkWriteSettingsPermission(context: Context): Either<OrientationError, Unit> =
        hasWriteSettingsPermission(context).fold(
            { error -> error.left() },
            { hasPermission ->
                if (hasPermission) Unit.right()
                else OrientationError.PermissionDenied("WRITE_SETTINGS").left()
            }
        )

    fun hasDrawOverlayPermission(context: Context): Either<OrientationError, Boolean> =
        Either.catch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true // Permission not required on pre-M devices
            }
        }.mapLeft {
            OrientationError.PermissionDenied("SYSTEM_ALERT_WINDOW")
        }

    fun checkDrawOverlayPermission(context: Context): Either<OrientationError, Unit> =
        hasDrawOverlayPermission(context).fold(
            { error -> error.left() },
            { hasPermission ->
                if (hasPermission) Unit.right()
                else OrientationError.PermissionDenied("SYSTEM_ALERT_WINDOW").left()
            }
        )
}
