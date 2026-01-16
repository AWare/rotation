package com.aware.rotation.domain.model

import org.junit.Assert.*
import org.junit.Test

class OrientationStateTest {

    @Test
    fun `default state has correct initial values`() {
        val state = OrientationState()

        assertEquals(ScreenOrientation.Unspecified, state.globalOrientation)
        assertNull(state.currentApp)
        assertTrue(state.perAppSettings.isEmpty())
        assertFalse(state.isAccessibilityServiceEnabled)
        assertFalse(state.hasDrawOverlayPermission)
    }

    @Test
    fun `withGlobalOrientation returns new state with updated orientation`() {
        val original = OrientationState()
        val updated = original.withGlobalOrientation(ScreenOrientation.Portrait)

        assertEquals(ScreenOrientation.Portrait, updated.globalOrientation)
        assertNotSame(original, updated)
    }

    @Test
    fun `withCurrentApp updates current app package name`() {
        val state = OrientationState().withCurrentApp("com.test.app")
        assertEquals("com.test.app", state.currentApp)
    }

    @Test
    fun `withPerAppSetting adds setting to map`() {
        val setting = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Landscape)
        val state = OrientationState().withPerAppSetting(setting)

        assertEquals(1, state.perAppSettings.size)
        assertEquals(setting, state.perAppSettings["com.test"])
    }

    @Test
    fun `withPerAppSetting replaces existing setting for same package`() {
        val setting1 = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Portrait)
        val setting2 = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Landscape)

        val state = OrientationState()
            .withPerAppSetting(setting1)
            .withPerAppSetting(setting2)

        assertEquals(1, state.perAppSettings.size)
        assertEquals(ScreenOrientation.Landscape, state.perAppSettings["com.test"]?.orientation)
    }

    @Test
    fun `removePerAppSetting removes setting from map`() {
        val setting = AppOrientationSetting.create("com.test", "Test")
        val state = OrientationState()
            .withPerAppSetting(setting)
            .removePerAppSetting("com.test")

        assertTrue(state.perAppSettings.isEmpty())
    }

    @Test
    fun `withAccessibilityServiceEnabled updates service status`() {
        val state = OrientationState().withAccessibilityServiceEnabled(true)
        assertTrue(state.isAccessibilityServiceEnabled)
    }

    @Test
    fun `withDrawOverlayPermission updates permission status`() {
        val state = OrientationState().withDrawOverlayPermission(true)
        assertTrue(state.hasDrawOverlayPermission)
    }

    @Test
    fun `getEffectiveOrientation returns per-app setting when available and enabled`() {
        val setting = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Landscape, enabled = true)
        val state = OrientationState(
            globalOrientation = ScreenOrientation.Portrait,
            currentApp = "com.test"
        ).withPerAppSetting(setting)

        assertEquals(ScreenOrientation.Landscape, state.getEffectiveOrientation("com.test"))
    }

    @Test
    fun `getEffectiveOrientation returns global when per-app setting disabled`() {
        val setting = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Landscape, enabled = false)
        val state = OrientationState(
            globalOrientation = ScreenOrientation.Portrait
        ).withPerAppSetting(setting)

        assertEquals(ScreenOrientation.Portrait, state.getEffectiveOrientation("com.test"))
    }

    @Test
    fun `getEffectiveOrientation returns global when no per-app setting exists`() {
        val state = OrientationState(globalOrientation = ScreenOrientation.Portrait)
        assertEquals(ScreenOrientation.Portrait, state.getEffectiveOrientation("com.test"))
    }

    @Test
    fun `getEffectiveOrientation uses current app when no package specified`() {
        val setting = AppOrientationSetting.create("com.test", "Test", ScreenOrientation.Landscape)
        val state = OrientationState(
            globalOrientation = ScreenOrientation.Portrait,
            currentApp = "com.test"
        ).withPerAppSetting(setting)

        assertEquals(ScreenOrientation.Landscape, state.getEffectiveOrientation())
    }

    @Test
    fun `isFullyConfigured returns true when all permissions granted`() {
        val state = OrientationState(
            isAccessibilityServiceEnabled = true,
            hasDrawOverlayPermission = true
        )
        assertTrue(state.isFullyConfigured())
    }

    @Test
    fun `isFullyConfigured returns false when permissions missing`() {
        val state1 = OrientationState(isAccessibilityServiceEnabled = true, hasDrawOverlayPermission = false)
        assertFalse(state1.isFullyConfigured())

        val state2 = OrientationState(isAccessibilityServiceEnabled = false, hasDrawOverlayPermission = true)
        assertFalse(state2.isFullyConfigured())

        val state3 = OrientationState(isAccessibilityServiceEnabled = false, hasDrawOverlayPermission = false)
        assertFalse(state3.isFullyConfigured())
    }

    @Test
    fun `state is immutable - modifications return new instances`() {
        val original = OrientationState()
        val modified = original.withGlobalOrientation(ScreenOrientation.Portrait)

        assertEquals(ScreenOrientation.Unspecified, original.globalOrientation)
        assertEquals(ScreenOrientation.Portrait, modified.globalOrientation)
    }
}
