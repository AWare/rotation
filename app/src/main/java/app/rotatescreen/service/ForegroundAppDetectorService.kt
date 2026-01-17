package app.rotatescreen.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import arrow.core.Either
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.repository.OrientationRepository
import app.rotatescreen.domain.model.TargetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Accessibility service that detects foreground app changes and applies per-app orientation settings
 */
class ForegroundAppDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: OrientationRepository
    private var currentPackageName: String? = null

    override fun onCreate() {
        super.onCreate()
        val database = RotationDatabase.getInstance(applicationContext)
        repository = OrientationRepository(database.appOrientationDao())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackageName) return

        currentPackageName = packageName
        applyOrientationForApp(packageName)
    }

    private fun applyOrientationForApp(packageName: String) {
        serviceScope.launch {
            repository.getSetting(packageName).fold(
                ifLeft = { /* No setting for this app, do nothing */ },
                ifRight = { setting ->
                    if (setting.enabled) {
                        val intent = Intent(
                            this@ForegroundAppDetectorService,
                            OrientationControlService::class.java
                        ).apply {
                            action = OrientationControlService.ACTION_SET_ORIENTATION
                            putExtra(OrientationControlService.EXTRA_ORIENTATION, setting.orientation.value)
                            putExtra(OrientationControlService.EXTRA_SCREEN_ID, setting.targetScreen.id)
                        }
                        startService(intent)
                    }
                }
            )
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
