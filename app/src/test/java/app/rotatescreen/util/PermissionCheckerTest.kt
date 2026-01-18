package app.rotatescreen.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import app.rotatescreen.domain.model.OrientationError
import arrow.core.Either
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PermissionChecker
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PermissionCheckerTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockkStatic(Settings.System::class)
        mockkStatic(Settings::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // WRITE_SETTINGS Permission Tests

    @Test
    fun `hasWriteSettingsPermission returns Right(true) when permission granted`() {
        every { Settings.System.canWrite(mockContext) } returns true

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { hasPermission -> assertTrue(hasPermission) }
        )
    }

    @Test
    fun `hasWriteSettingsPermission returns Right(false) when permission denied`() {
        every { Settings.System.canWrite(mockContext) } returns false

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { hasPermission -> assertFalse(hasPermission) }
        )
    }

    @Test
    fun `hasWriteSettingsPermission returns Left when exception thrown`() {
        every { Settings.System.canWrite(mockContext) } throws SecurityException("Test exception")

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.PermissionDenied)
                assertEquals("WRITE_SETTINGS", (error as OrientationError.PermissionDenied).permission)
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    fun `checkWriteSettingsPermission returns Right(Unit) when permission granted`() {
        every { Settings.System.canWrite(mockContext) } returns true

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { assertEquals(Unit, it) }
        )
    }

    @Test
    fun `checkWriteSettingsPermission returns Left when permission denied`() {
        every { Settings.System.canWrite(mockContext) } returns false

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.PermissionDenied)
                assertEquals("WRITE_SETTINGS", (error as OrientationError.PermissionDenied).permission)
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    fun `checkWriteSettingsPermission returns Left when exception thrown`() {
        every { Settings.System.canWrite(mockContext) } throws RuntimeException("Test error")

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
    }

    // DRAW_OVERLAY Permission Tests

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `hasDrawOverlayPermission returns Right(true) when permission granted on M+`() {
        every { Settings.canDrawOverlays(mockContext) } returns true

        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { hasPermission -> assertTrue(hasPermission) }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `hasDrawOverlayPermission returns Right(false) when permission denied on M+`() {
        every { Settings.canDrawOverlays(mockContext) } returns false

        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { hasPermission -> assertFalse(hasPermission) }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `hasDrawOverlayPermission returns Right(true) on pre-M devices`() {
        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { hasPermission -> assertTrue(hasPermission) }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `hasDrawOverlayPermission returns Left when exception thrown`() {
        every { Settings.canDrawOverlays(mockContext) } throws SecurityException("Test exception")

        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.PermissionDenied)
                assertEquals("SYSTEM_ALERT_WINDOW", (error as OrientationError.PermissionDenied).permission)
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `checkDrawOverlayPermission returns Right(Unit) when permission granted`() {
        every { Settings.canDrawOverlays(mockContext) } returns true

        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { assertEquals(Unit, it) }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `checkDrawOverlayPermission returns Left when permission denied`() {
        every { Settings.canDrawOverlays(mockContext) } returns false

        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.PermissionDenied)
                assertEquals("SYSTEM_ALERT_WINDOW", (error as OrientationError.PermissionDenied).permission)
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `checkDrawOverlayPermission returns Right(Unit) on pre-M devices`() {
        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `checkDrawOverlayPermission returns Left when exception thrown`() {
        every { Settings.canDrawOverlays(mockContext) } throws RuntimeException("Test error")

        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isLeft())
    }

    // Edge Cases

    @Test
    fun `multiple sequential calls work correctly`() {
        every { Settings.System.canWrite(mockContext) } returns true
        every { Settings.canDrawOverlays(mockContext) } returns true

        val result1 = PermissionChecker.hasWriteSettingsPermission(mockContext)
        val result2 = PermissionChecker.hasDrawOverlayPermission(mockContext)
        val result3 = PermissionChecker.checkWriteSettingsPermission(mockContext)
        val result4 = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())
        assertTrue(result3.isRight())
        assertTrue(result4.isRight())
    }
}
