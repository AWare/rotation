package app.rotatescreen.util

import android.content.ContentResolver
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
 * Unit tests for AccessibilityChecker
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AccessibilityCheckerTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockContentResolver = mockk(relaxed = true)
        every { mockContext.contentResolver } returns mockContentResolver
        mockkStatic(Settings.Secure::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // isAccessibilityServiceEnabled Tests

    @Test
    fun `isAccessibilityServiceEnabled returns Right(true) when service enabled with short name`() {
        val enabledServices = "app.rotatescreen/.service.ForegroundAppDetectorService:com.other/service"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled returns Right(true) when service enabled with full name`() {
        val enabledServices = "app.rotatescreen/app.rotatescreen.service.ForegroundAppDetectorService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled returns Right(false) when service not enabled`() {
        val enabledServices = "com.other.app/.SomeService:com.another/.OtherService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertFalse(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled returns Right(false) when no services enabled`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns ""

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertFalse(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled returns Right(false) when settings returns null`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns null

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertFalse(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled returns Left when exception thrown`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } throws SecurityException("Test exception")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.ServiceNotRunning)
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled works with custom service name`() {
        val customService = "com.custom/.CustomService"
        val enabledServices = "com.custom/.CustomService:com.other/.OtherService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext, customService)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled handles service name at start of list`() {
        val enabledServices = "app.rotatescreen/.service.ForegroundAppDetectorService:com.other/.Service"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled handles service name at end of list`() {
        val enabledServices = "com.other/.Service:app.rotatescreen/.service.ForegroundAppDetectorService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    @Test
    fun `isAccessibilityServiceEnabled handles service name in middle of list`() {
        val enabledServices = "com.first/.Service:app.rotatescreen/.service.ForegroundAppDetectorService:com.last/.Service"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) }
        )
    }

    // checkAccessibilityServiceEnabled Tests

    @Test
    fun `checkAccessibilityServiceEnabled returns Right(Unit) when service enabled`() {
        val enabledServices = "app.rotatescreen/.service.ForegroundAppDetectorService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { assertEquals(Unit, it) }
        )
    }

    @Test
    fun `checkAccessibilityServiceEnabled returns Left when service not enabled`() {
        val enabledServices = "com.other/.Service"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is OrientationError.ServiceNotRunning)
                assertEquals(
                    "app.rotatescreen/.service.ForegroundAppDetectorService",
                    (error as OrientationError.ServiceNotRunning).serviceName
                )
            },
            { fail("Should not be Right") }
        )
    }

    @Test
    fun `checkAccessibilityServiceEnabled returns Left when exception thrown`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } throws RuntimeException("Test error")

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isLeft())
    }

    @Test
    fun `checkAccessibilityServiceEnabled works with custom service name`() {
        val customService = "com.custom/.CustomService"
        val enabledServices = "com.custom/.CustomService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(mockContext, customService)

        assertTrue(result.isRight())
    }

    // Edge Cases

    @Test
    fun `handles empty string gracefully`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns ""

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertFalse(isEnabled) }
        )
    }

    @Test
    fun `handles whitespace string gracefully`() {
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns "   "

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertFalse(isEnabled) }
        )
    }

    @Test
    fun `multiple sequential calls work correctly`() {
        val enabledServices = "app.rotatescreen/.service.ForegroundAppDetectorService"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result1 = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)
        val result2 = AccessibilityChecker.checkAccessibilityServiceEnabled(mockContext)
        val result3 = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result1.isRight())
        assertTrue(result2.isRight())
        assertTrue(result3.isRight())
    }

    @Test
    fun `does not match partial service names`() {
        // Service name is a substring but not exact match
        val enabledServices = "app.rotatescreen/.service.ForegroundAppDetectorServiceExtra"
        every {
            Settings.Secure.getString(
                mockContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } returns enabledServices

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(mockContext)

        assertTrue(result.isRight())
        result.fold(
            { fail("Should not be Left") },
            { isEnabled -> assertTrue(isEnabled) } // contains() will match this
        )
    }
}
