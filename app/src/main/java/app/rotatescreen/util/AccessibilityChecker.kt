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
            // First check if accessibility is enabled at all
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            ) == 1

            if (!accessibilityEnabled) {
                return@catch false
            }

            // Get the colon-separated list of enabled services
            val enabledServicesString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            if (enabledServicesString.isEmpty()) {
                return@catch false
            }

            // Parse the colon-separated list
            val enabledServices = enabledServicesString.split(":")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // Use dynamic package name to support both debug and release builds
            val packageName = context.packageName
            val serviceClassName = ".service.ForegroundAppDetectorService"

            // Check for multiple possible formats
            val possibleNames = listOf(
                "$packageName/$serviceClassName",
                "$packageName/$packageName.service.ForegroundAppDetectorService",
                "$packageName/service.ForegroundAppDetectorService"
            )

            // Check if any of our service names is in the enabled list (exact match)
            enabledServices.any { enabledService ->
                possibleNames.any { possibleName ->
                    enabledService.equals(possibleName, ignoreCase = true)
                }
            }
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
