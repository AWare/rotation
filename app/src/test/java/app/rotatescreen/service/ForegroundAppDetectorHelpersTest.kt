package app.rotatescreen.service

import android.view.accessibility.AccessibilityEvent
import app.rotatescreen.domain.model.AppOrientationSetting
import app.rotatescreen.domain.model.ScreenOrientation
import app.rotatescreen.domain.model.TargetScreen
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive tests for ForegroundAppDetectorHelpers
 * Tests all pure helper functions for correctness
 */
class ForegroundAppDetectorHelpersTest {

    // ========== extractPackageName Tests ==========

    @Test
    fun `extractPackageName returns package name for window state changed event`() {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns "com.test.app"

        val result = ForegroundAppDetectorHelpers.extractPackageName(event)

        assertEquals("com.test.app", result)
    }

    @Test
    fun `extractPackageName returns null for non-window-state-changed event`() {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.test.app"

        val result = ForegroundAppDetectorHelpers.extractPackageName(event)

        assertNull(result)
    }

    @Test
    fun `extractPackageName returns null for null event`() {
        val result = ForegroundAppDetectorHelpers.extractPackageName(null)

        assertNull(result)
    }

    @Test
    fun `extractPackageName handles null package name`() {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns null

        val result = ForegroundAppDetectorHelpers.extractPackageName(event)

        assertNull(result)
    }

    // ========== shouldDebounce Tests ==========

    @Test
    fun `shouldDebounce returns true when time difference is less than delay`() {
        val currentTime = 1000L
        val lastTime = 900L
        val debounceDelay = 150L

        val result = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)

        assertTrue(result)
    }

    @Test
    fun `shouldDebounce returns false when time difference equals delay`() {
        val currentTime = 1000L
        val lastTime = 850L
        val debounceDelay = 150L

        val result = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)

        assertFalse(result)
    }

    @Test
    fun `shouldDebounce returns false when time difference is greater than delay`() {
        val currentTime = 1000L
        val lastTime = 800L
        val debounceDelay = 150L

        val result = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)

        assertFalse(result)
    }

    @Test
    fun `shouldDebounce handles zero delay`() {
        val currentTime = 1000L
        val lastTime = 1000L
        val debounceDelay = 0L

        val result = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)

        assertFalse(result)
    }

    @Test
    fun `shouldDebounce handles very large time differences`() {
        val currentTime = Long.MAX_VALUE
        val lastTime = 0L
        val debounceDelay = 150L

        val result = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)

        assertFalse(result)
    }

    @Test
    fun `shouldDebounce boundary test - exactly at boundary`() {
        val debounceDelay = 150L
        val lastTime = 1000L
        val currentTime = lastTime + debounceDelay - 1

        val resultJustBefore = ForegroundAppDetectorHelpers.shouldDebounce(currentTime, lastTime, debounceDelay)
        val resultExactly = ForegroundAppDetectorHelpers.shouldDebounce(currentTime + 1, lastTime, debounceDelay)

        assertTrue(resultJustBefore)
        assertFalse(resultExactly)
    }

    // ========== isSamePackage Tests ==========

    @Test
    fun `isSamePackage returns true for identical non-null packages`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage("com.test.app", "com.test.app")

        assertTrue(result)
    }

    @Test
    fun `isSamePackage returns false for different packages`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage("com.test.app1", "com.test.app2")

        assertFalse(result)
    }

    @Test
    fun `isSamePackage returns true for both null`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage(null, null)

        assertTrue(result)
    }

    @Test
    fun `isSamePackage returns false when first is null`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage(null, "com.test.app")

        assertFalse(result)
    }

    @Test
    fun `isSamePackage returns false when second is null`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage("com.test.app", null)

        assertFalse(result)
    }

    @Test
    fun `isSamePackage is case sensitive`() {
        val result = ForegroundAppDetectorHelpers.isSamePackage("com.test.App", "com.test.app")

        assertFalse(result)
    }

    // ========== isLauncherPackage Tests ==========

    @Test
    fun `isLauncherPackage returns true when package is in launcher set`() {
        val launchers = setOf("com.launcher1", "com.launcher2", "com.launcher3")

        val result = ForegroundAppDetectorHelpers.isLauncherPackage("com.launcher2", launchers)

        assertTrue(result)
    }

    @Test
    fun `isLauncherPackage returns false when package is not in launcher set`() {
        val launchers = setOf("com.launcher1", "com.launcher2", "com.launcher3")

        val result = ForegroundAppDetectorHelpers.isLauncherPackage("com.other.app", launchers)

        assertFalse(result)
    }

    @Test
    fun `isLauncherPackage returns false for empty launcher set`() {
        val launchers = emptySet<String>()

        val result = ForegroundAppDetectorHelpers.isLauncherPackage("com.launcher", launchers)

        assertFalse(result)
    }

    @Test
    fun `isLauncherPackage is case sensitive`() {
        val launchers = setOf("com.Launcher")

        val result = ForegroundAppDetectorHelpers.isLauncherPackage("com.launcher", launchers)

        assertFalse(result)
    }

    @Test
    fun `isLauncherPackage handles single launcher`() {
        val launchers = setOf("com.only.launcher")

        val resultMatch = ForegroundAppDetectorHelpers.isLauncherPackage("com.only.launcher", launchers)
        val resultNoMatch = ForegroundAppDetectorHelpers.isLauncherPackage("com.other.app", launchers)

        assertTrue(resultMatch)
        assertFalse(resultNoMatch)
    }

    // ========== isLeavingApp Tests ==========

    @Test
    fun `isLeavingApp returns true when previous is non-null and different from current`() {
        val result = ForegroundAppDetectorHelpers.isLeavingApp("com.previous.app", "com.current.app")

        assertTrue(result)
    }

    @Test
    fun `isLeavingApp returns false when previous is null`() {
        val result = ForegroundAppDetectorHelpers.isLeavingApp(null, "com.current.app")

        assertFalse(result)
    }

    @Test
    fun `isLeavingApp returns false when previous and current are the same`() {
        val result = ForegroundAppDetectorHelpers.isLeavingApp("com.same.app", "com.same.app")

        assertFalse(result)
    }

    @Test
    fun `isLeavingApp is case sensitive`() {
        val result = ForegroundAppDetectorHelpers.isLeavingApp("com.App", "com.app")

        assertTrue(result)
    }

    // ========== hasCustomOrientation Tests ==========

    @Test
    fun `hasCustomOrientation returns true when at least one setting is enabled`() {
        val settings = listOf(
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Portrait,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = true),
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Landscape,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = false)
        )

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertTrue(result)
    }

    @Test
    fun `hasCustomOrientation returns false when all settings are disabled`() {
        val settings = listOf(
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Portrait,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = false),
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Landscape,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = false)
        )

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertFalse(result)
    }

    @Test
    fun `hasCustomOrientation returns false for empty settings list`() {
        val settings = emptyList<AppOrientationSetting>()

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertFalse(result)
    }

    @Test
    fun `hasCustomOrientation returns true when all settings are enabled`() {
        val settings = listOf(
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Portrait,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = true),
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Landscape,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = true)
        )

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertTrue(result)
    }

    @Test
    fun `hasCustomOrientation handles single enabled setting`() {
        val settings = listOf(
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Portrait,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = true)
        )

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertTrue(result)
    }

    @Test
    fun `hasCustomOrientation handles single disabled setting`() {
        val settings = listOf(
            AppOrientationSetting.create(
                packageName = "com.test.app",
                appName = "Test App",
                orientation = ScreenOrientation.Portrait,
                targetScreen = TargetScreen.AllScreens
            ).copy(enabled = false)
        )

        val result = ForegroundAppDetectorHelpers.hasCustomOrientation(settings)

        assertFalse(result)
    }

    // ========== isSystemLauncher Tests ==========

    @Test
    fun `isSystemLauncher returns true for known AOSP launcher`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.android.launcher3")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher returns true for Google Pixel launcher`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.google.android.apps.nexuslauncher")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher returns true for Nova Launcher`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.teslacoilsw.launcher")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher returns true for Lawnchair`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("ch.deletescape.lawnchair.plah")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher returns true for package containing launcher`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.somevendor.launcher.app")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher is case insensitive`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.somevendor.LAUNCHER.app")

        assertTrue(result)
    }

    @Test
    fun `isSystemLauncher returns false for regular app`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.example.app")

        assertFalse(result)
    }

    @Test
    fun `isSystemLauncher returns false for app without launcher in name`() {
        val result = ForegroundAppDetectorHelpers.isSystemLauncher("com.example.camera")

        assertFalse(result)
    }
}
