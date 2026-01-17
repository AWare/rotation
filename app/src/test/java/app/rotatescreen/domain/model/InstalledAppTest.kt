package app.rotatescreen.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstalledAppTest {

    @Test
    fun `InstalledApp defaults to installed`() {
        val app = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App"
        )

        assertTrue(app.isInstalled)
    }

    @Test
    fun `InstalledApp can be marked as not installed`() {
        val app = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            isInstalled = false
        )

        assertFalse(app.isInstalled)
    }

    @Test
    fun `InstalledApp defaults to not recent`() {
        val app = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App"
        )

        assertFalse(app.isRecent)
    }

    @Test
    fun `InstalledApp can be marked as recent`() {
        val app = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            isRecent = true
        )

        assertTrue(app.isRecent)
    }

    @Test
    fun `InstalledApp with all properties`() {
        val app = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            iconPath = "/path/to/icon",
            isRecent = true,
            isInstalled = false
        )

        assertEquals("com.example.app", app.packageName)
        assertEquals("Test App", app.appName)
        assertEquals("/path/to/icon", app.iconPath)
        assertTrue(app.isRecent)
        assertFalse(app.isInstalled)
    }

    @Test
    fun `two InstalledApp instances with same data are equal`() {
        val app1 = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            isRecent = true,
            isInstalled = true
        )

        val app2 = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            isRecent = true,
            isInstalled = true
        )

        assertEquals(app1, app2)
    }

    @Test
    fun `InstalledApp copy works correctly`() {
        val original = InstalledApp(
            packageName = "com.example.app",
            appName = "Test App",
            isInstalled = true
        )

        val copy = original.copy(isInstalled = false)

        assertEquals(original.packageName, copy.packageName)
        assertEquals(original.appName, copy.appName)
        assertTrue(original.isInstalled)
        assertFalse(copy.isInstalled)
    }
}
