package com.aware.rotation.util

import android.content.Context
import android.provider.Settings
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.aware.rotation.domain.model.OrientationError

/**
 * Utility for checking accessibility service status using FP style
 */
object AccessibilityChecker {
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceName: String = "com.aware.rotation/.service.ForegroundAppDetectorService"
    ): Either<OrientationError, Boolean> =
        Either.catch {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.contains(serviceName)
        }.mapLeft {
            OrientationError.ServiceNotRunning(serviceName)
        }

    fun checkAccessibilityServiceEnabled(
        context: Context,
        serviceName: String = "com.aware.rotation/.service.ForegroundAppDetectorService"
    ): Either<OrientationError, Unit> =
        isAccessibilityServiceEnabled(context, serviceName).flatMap { isEnabled ->
            if (isEnabled) Unit.right()
            else OrientationError.ServiceNotRunning(serviceName).left()
        }
}
