package app.rotatescreen.util

import android.content.Context
import android.provider.Settings
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import app.rotatescreen.domain.model.OrientationError

/**
 * Utility for checking accessibility service status using FP style
 */
object AccessibilityChecker {
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceName: String = "app.rotatescreen/.service.ForegroundAppDetectorService"
    ): Either<OrientationError, Boolean> =
        Either.catch {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            // Check for multiple possible formats
            // Format 1: app.rotatescreen/.service.ForegroundAppDetectorService
            // Format 2: app.rotatescreen/app.rotatescreen.service.ForegroundAppDetectorService
            val shortName = "app.rotatescreen/.service.ForegroundAppDetectorService"
            val fullName = "app.rotatescreen/app.rotatescreen.service.ForegroundAppDetectorService"

            enabledServices.contains(shortName) || enabledServices.contains(fullName)
        }.mapLeft {
            OrientationError.ServiceNotRunning(serviceName)
        }

    fun checkAccessibilityServiceEnabled(
        context: Context,
        serviceName: String = "app.rotatescreen/.service.ForegroundAppDetectorService"
    ): Either<OrientationError, Unit> =
        isAccessibilityServiceEnabled(context, serviceName).fold(
            { error -> error.left() },
            { isEnabled ->
                if (isEnabled) Unit.right()
                else OrientationError.ServiceNotRunning(serviceName).left()
            }
        )
}
