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
        serviceName: String? = null
    ): Either<OrientationError, Boolean> =
        Either.catch {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            // Use dynamic package name to support both debug and release builds
            val packageName = context.packageName
            val serviceClassName = ".service.ForegroundAppDetectorService"

            // Check for multiple possible formats
            // Format 1: app.rotatescreen/.service.ForegroundAppDetectorService (or app.rotatescreen.debug/...)
            // Format 2: app.rotatescreen/app.rotatescreen.service.ForegroundAppDetectorService (or app.rotatescreen.debug/...)
            val shortName = "$packageName/$serviceClassName"
            val fullName = "$packageName/$packageName.service.ForegroundAppDetectorService"

            enabledServices.contains(shortName) || enabledServices.contains(fullName)
        }.mapLeft {
            OrientationError.ServiceNotRunning(serviceName ?: "ForegroundAppDetectorService")
        }

    fun checkAccessibilityServiceEnabled(
        context: Context,
        serviceName: String? = null
    ): Either<OrientationError, Unit> =
        isAccessibilityServiceEnabled(context, serviceName).fold(
            { error -> error.left() },
            { isEnabled ->
                if (isEnabled) Unit.right()
                else OrientationError.ServiceNotRunning(serviceName ?: "ForegroundAppDetectorService").left()
            }
        )
}
