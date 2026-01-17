package app.rotatescreen.domain.model

import org.junit.Assert.*
import org.junit.Test

class AppOrientationSettingTest {

    @Test
    fun `create produces correct default values`() {
        val setting = AppOrientationSetting.create(
            packageName = "com.test.app",
            appName = "Test App"
        )

        assertEquals("com.test.app", setting.packageName)
        assertEquals("Test App", setting.appName)
        assertEquals(ScreenOrientation.Unspecified, setting.orientation)
        assertEquals(TargetScreen.AllScreens, setting.targetScreen)
        assertTrue(setting.enabled)
    }

    @Test
    fun `withOrientation returns new instance with updated orientation`() {
        val original = AppOrientationSetting.create("com.test", "Test")
        val updated = original.withOrientation(ScreenOrientation.Portrait)

        assertEquals(ScreenOrientation.Portrait, updated.orientation)
        assertEquals(original.packageName, updated.packageName)
        assertEquals(original.appName, updated.appName)
        assertNotSame(original, updated)
    }

    @Test
    fun `withTargetScreen returns new instance with updated screen`() {
        val original = AppOrientationSetting.create("com.test", "Test")
        val newScreen = TargetScreen.SpecificScreen.primary()
        val updated = original.withTargetScreen(newScreen)

        assertEquals(newScreen, updated.targetScreen)
        assertEquals(original.packageName, updated.packageName)
        assertNotSame(original, updated)
    }

    @Test
    fun `toggleEnabled switches enabled state`() {
        val original = AppOrientationSetting.create("com.test", "Test", enabled = true)
        val disabled = original.toggleEnabled()
        assertFalse(disabled.enabled)

        val reEnabled = disabled.toggleEnabled()
        assertTrue(reEnabled.enabled)
    }

    @Test
    fun `copy creates new instance preserving immutability`() {
        val original = AppOrientationSetting.create("com.test", "Test")
        val copy = original.copy(appName = "Updated Name")

        assertEquals("Updated Name", copy.appName)
        assertEquals("Test", original.appName)
        assertNotSame(original, copy)
    }

    @Test
    fun `InstalledApp data class works correctly`() {
        val app = InstalledApp(
            packageName = "com.test",
            appName = "Test",
            iconPath = "/path/to/icon"
        )

        assertEquals("com.test", app.packageName)
        assertEquals("Test", app.appName)
        assertEquals("/path/to/icon", app.iconPath)
    }
}
