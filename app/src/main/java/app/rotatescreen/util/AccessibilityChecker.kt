package app.rotatescreen.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import app.rotatescreen.service.ForegroundAppDetectorService
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import app.rotatescreen.domain.model.OrientationError

/**
 * Utility for checking accessibility service status using FP style
 * Uses AccessibilityManager for reliable detection
 */
object AccessibilityChecker {
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceName: String? = null
    ): Either<OrientationError, Boolean> =
        Either.catch {
            // Get AccessibilityManager system service
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager
                ?: return@catch false

            // Check if accessibility is enabled at all
            if (!accessibilityManager.isEnabled) {
                return@catch false
            }

            // Get list of enabled accessibility services
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

            if (enabledServices.isEmpty()) {
                return@catch false
            }

            // Build our service ComponentName
            val ourServiceComponent = ComponentName(
                context,
                ForegroundAppDetectorService::class.java
            )

            // Check if our service is in the enabled list
            enabledServices.any { serviceInfo ->
                val serviceId = serviceInfo.id
                val componentFromId = ComponentName.unflattenFromString(serviceId)

                // Compare ComponentNames
                componentFromId == ourServiceComponent
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
