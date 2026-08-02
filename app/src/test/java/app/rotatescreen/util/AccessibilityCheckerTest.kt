package app.rotatescreen.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import app.rotatescreen.domain.model.OrientationError
import app.rotatescreen.service.ForegroundAppDetectorService
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
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

/**
 * Unit tests for AccessibilityChecker.
 *
 * The checker asks AccessibilityManager for the enabled service list and
 * compares ComponentNames, so these tests drive it through a mocked manager
 * rather than through the Settings.Secure string it used to parse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AccessibilityCheckerTest {

    private val packageName = "app.rotatescreen"

    private lateinit var context: Context
    private lateinit var accessibilityManager: AccessibilityManager

    /** The flattened id AccessibilityManager reports for our own service. */
    private val ourServiceId: String
        get() = ComponentName(packageName, ForegroundAppDetectorService::class.java.name)
            .flattenToString()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        accessibilityManager = mockk(relaxed = true)

        every { context.packageName } returns packageName
        every { context.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns accessibilityManager
        every { accessibilityManager.isEnabled } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun serviceInfo(id: String): AccessibilityServiceInfo =
        mockk<AccessibilityServiceInfo>(relaxed = true).also { every { it.id } returns id }

    private fun enabledServices(vararg ids: String) {
        every {
            accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
        } returns ids.map(::serviceInfo)
    }

    // isAccessibilityServiceEnabled

    @Test
    fun `returns Right(true) when our service is the only one enabled`() {
        enabledServices(ourServiceId)

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `returns Right(true) when our service is enabled alongside others`() {
        enabledServices(
            "com.other.app/com.other.app.SomeService",
            ourServiceId,
            "com.third.app/.AnotherService"
        )

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `returns Right(true) for the shorthand relative class form`() {
        // ComponentName.unflattenFromString expands a leading dot against the
        // package, so "pkg/.Service" must match "pkg/pkg.Service".
        enabledServices("$packageName/.service.ForegroundAppDetectorService")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `returns Right(false) when only other services are enabled`() {
        enabledServices(
            "com.other.app/com.other.app.SomeService",
            "com.third.app/.AnotherService"
        )

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `returns Right(false) when no services are enabled`() {
        enabledServices()

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `returns Right(false) when accessibility is switched off entirely`() {
        every { accessibilityManager.isEnabled } returns false
        enabledServices(ourServiceId)

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `returns Right(false) when the accessibility service is unavailable`() {
        every { context.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns null

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `ignores malformed service ids without failing`() {
        // unflattenFromString returns null for an id with no '/' separator.
        enabledServices("not-a-component-name", ourServiceId)

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `does not match a same-named service from another package`() {
        enabledServices("com.impostor/app.rotatescreen.service.ForegroundAppDetectorService")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `returns Left when the manager throws`() {
        every {
            accessibilityManager.getEnabledAccessibilityServiceList(any())
        } throws SecurityException("denied")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertTrue(result.isLeft())
        assertEquals(
            OrientationError.ServiceNotRunning("ForegroundAppDetectorService"),
            result.leftOrNull()
        )
    }

    @Test
    fun `Left carries the supplied service name`() {
        every {
            accessibilityManager.getEnabledAccessibilityServiceList(any())
        } throws SecurityException("denied")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context, "CustomService")

        assertEquals(
            OrientationError.ServiceNotRunning("CustomService"),
            result.leftOrNull()
        )
    }

    @Test
    fun `queries the manager with the all-feedback mask`() {
        enabledServices(ourServiceId)

        AccessibilityChecker.isAccessibilityServiceEnabled(context)

        verify {
            accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
        }
    }

    @Test
    fun `does not report enabled when every id is malformed`() {
        enabledServices("garbage", "also/", "/nope")

        val result = AccessibilityChecker.isAccessibilityServiceEnabled(context)

        assertFalse(result.getOrNull() ?: true)
    }

    // checkAccessibilityServiceEnabled

    @Test
    fun `check returns Right(Unit) when our service is enabled`() {
        enabledServices(ourServiceId)

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(context)

        assertTrue(result.isRight())
    }

    @Test
    fun `check returns Left when our service is not enabled`() {
        enabledServices("com.other.app/com.other.app.SomeService")

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(context)

        assertTrue(result.isLeft())
        assertEquals(
            OrientationError.ServiceNotRunning("ForegroundAppDetectorService"),
            result.leftOrNull()
        )
    }

    @Test
    fun `check returns Left with the supplied service name when not enabled`() {
        enabledServices()

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(context, "CustomService")

        assertEquals(
            OrientationError.ServiceNotRunning("CustomService"),
            result.leftOrNull()
        )
    }

    @Test
    fun `check propagates a Left from the underlying query`() {
        every {
            accessibilityManager.getEnabledAccessibilityServiceList(any())
        } throws SecurityException("denied")

        val result = AccessibilityChecker.checkAccessibilityServiceEnabled(context)

        assertTrue(result.isLeft())
        result.fold(
            { error -> assertTrue(error is OrientationError.ServiceNotRunning) },
            { fail("Should not be Right") }
        )
    }
}
