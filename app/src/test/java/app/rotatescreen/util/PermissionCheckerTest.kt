package app.rotatescreen.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import app.rotatescreen.domain.model.OrientationError
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/**
 * Unit tests for PermissionChecker.
 *
 * The SDK level is left at the class-level default: the app's minSdk is 29, so
 * Robolectric cannot build an environment for anything lower (it fails parsing
 * the manifest), and the pre-M branch of hasDrawOverlayPermission is therefore
 * unreachable on any supported device.
 *
 * Settings.canDrawOverlays is driven through Robolectric's own shadow rather
 * than a mockk static: mocking both Settings and Settings.System at once
 * fights Robolectric's instrumentation of the outer class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], shadows = [ShadowWritableSystemSettings::class])
class PermissionCheckerTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        ShadowSettings.reset()
        ShadowWritableSystemSettings.resetCanWrite()
    }

    // WRITE_SETTINGS

    @Test
    fun `hasWriteSettingsPermission returns Right(true) when permission granted`() {
        ShadowWritableSystemSettings.canWrite = true

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `hasWriteSettingsPermission returns Right(false) when permission denied`() {
        ShadowWritableSystemSettings.canWrite = false

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `hasWriteSettingsPermission returns Left when exception thrown`() {
        ShadowWritableSystemSettings.error = SecurityException("Test exception")

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
        assertEquals(
            OrientationError.PermissionDenied("WRITE_SETTINGS"),
            result.leftOrNull()
        )
    }

    @Test
    fun `checkWriteSettingsPermission returns Right(Unit) when permission granted`() {
        ShadowWritableSystemSettings.canWrite = true

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `checkWriteSettingsPermission returns Left when permission denied`() {
        ShadowWritableSystemSettings.canWrite = false

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
        assertEquals(
            OrientationError.PermissionDenied("WRITE_SETTINGS"),
            result.leftOrNull()
        )
    }

    @Test
    fun `checkWriteSettingsPermission returns Left when exception thrown`() {
        ShadowWritableSystemSettings.error = RuntimeException("Test error")

        val result = PermissionChecker.checkWriteSettingsPermission(mockContext)

        assertTrue(result.isLeft())
    }

    // DRAW_OVERLAY

    @Test
    fun `hasDrawOverlayPermission returns Right(true) when permission granted`() {
        ShadowSettings.setCanDrawOverlays(true)

        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `hasDrawOverlayPermission returns Right(false) when permission denied`() {
        ShadowSettings.setCanDrawOverlays(false)

        val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `hasDrawOverlayPermission returns Left when exception thrown`() {
        mockkStatic(Settings::class)
        try {
            every { Settings.canDrawOverlays(mockContext) } throws SecurityException("Test exception")

            val result = PermissionChecker.hasDrawOverlayPermission(mockContext)

            assertTrue(result.isLeft())
            assertEquals(
                OrientationError.PermissionDenied("SYSTEM_ALERT_WINDOW"),
                result.leftOrNull()
            )
        } finally {
            unmockkStatic(Settings::class)
        }
    }

    @Test
    fun `checkDrawOverlayPermission returns Right(Unit) when permission granted`() {
        ShadowSettings.setCanDrawOverlays(true)

        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isRight())
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `checkDrawOverlayPermission returns Left when permission denied`() {
        ShadowSettings.setCanDrawOverlays(false)

        val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result.isLeft())
        assertEquals(
            OrientationError.PermissionDenied("SYSTEM_ALERT_WINDOW"),
            result.leftOrNull()
        )
    }

    @Test
    fun `checkDrawOverlayPermission returns Left when exception thrown`() {
        mockkStatic(Settings::class)
        try {
            every { Settings.canDrawOverlays(mockContext) } throws RuntimeException("Test error")

            val result = PermissionChecker.checkDrawOverlayPermission(mockContext)

            assertTrue(result.isLeft())
        } finally {
            unmockkStatic(Settings::class)
        }
    }

    // Edge cases

    @Test
    fun `write and overlay permissions are reported independently`() {
        ShadowWritableSystemSettings.canWrite = true
        ShadowSettings.setCanDrawOverlays(false)

        assertEquals(true, PermissionChecker.hasWriteSettingsPermission(mockContext).getOrNull())
        assertEquals(false, PermissionChecker.hasDrawOverlayPermission(mockContext).getOrNull())
    }

    @Test
    fun `multiple sequential calls work correctly`() {
        ShadowWritableSystemSettings.canWrite = true
        ShadowSettings.setCanDrawOverlays(true)

        val result1 = PermissionChecker.hasWriteSettingsPermission(mockContext)
        val result2 = PermissionChecker.hasDrawOverlayPermission(mockContext)
        val result3 = PermissionChecker.checkWriteSettingsPermission(mockContext)
        val result4 = PermissionChecker.checkDrawOverlayPermission(mockContext)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())
        assertTrue(result3.isRight())
        assertTrue(result4.isRight())
    }

    @Test
    fun `a denied write permission does not surface as an error`() {
        ShadowWritableSystemSettings.canWrite = false

        val result = PermissionChecker.hasWriteSettingsPermission(mockContext)

        assertFalse(result.isLeft())
        result.fold({ fail("Should not be Left") }, { assertFalse(it) })
    }
}
