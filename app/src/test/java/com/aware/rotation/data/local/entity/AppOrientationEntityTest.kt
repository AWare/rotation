package com.aware.rotation.data.local.entity

import android.content.pm.ActivityInfo
import com.aware.rotation.domain.model.AppOrientationSetting
import com.aware.rotation.domain.model.ScreenOrientation
import com.aware.rotation.domain.model.TargetScreen
import org.junit.Assert.*
import org.junit.Test

class AppOrientationEntityTest {

    @Test
    fun `toDomain converts entity to domain model correctly`() {
        val entity = AppOrientationEntity(
            packageName = "com.test.app",
            appName = "Test App",
            orientationValue = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            targetScreenId = -1,
            targetScreenName = "All Screens",
            enabled = true
        )

        val domain = entity.toDomain()

        assertEquals("com.test.app", domain.packageName)
        assertEquals("Test App", domain.appName)
        assertEquals(ScreenOrientation.Portrait, domain.orientation)
        assertEquals(TargetScreen.AllScreens, domain.targetScreen)
        assertTrue(domain.enabled)
    }

    @Test
    fun `toDomain handles invalid orientation gracefully`() {
        val entity = AppOrientationEntity(
            packageName = "com.test",
            appName = "Test",
            orientationValue = 9999, // Invalid
            targetScreenId = -1,
            targetScreenName = "All Screens",
            enabled = true
        )

        val domain = entity.toDomain()
        assertEquals(ScreenOrientation.Unspecified, domain.orientation)
    }

    @Test
    fun `toDomain handles invalid screen id gracefully`() {
        val entity = AppOrientationEntity(
            packageName = "com.test",
            appName = "Test",
            orientationValue = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            targetScreenId = -999, // Invalid
            targetScreenName = "Invalid",
            enabled = true
        )

        val domain = entity.toDomain()
        assertEquals(TargetScreen.AllScreens, domain.targetScreen)
    }

    @Test
    fun `toEntity converts domain model to entity correctly`() {
        val domain = AppOrientationSetting.create(
            packageName = "com.test.app",
            appName = "Test App",
            orientation = ScreenOrientation.Landscape,
            targetScreen = TargetScreen.SpecificScreen.primary(),
            enabled = false
        )

        val entity = domain.toEntity()

        assertEquals("com.test.app", entity.packageName)
        assertEquals("Test App", entity.appName)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, entity.orientationValue)
        assertEquals(0, entity.targetScreenId)
        assertFalse(entity.enabled)
    }

    @Test
    fun `roundtrip conversion preserves data`() {
        val original = AppOrientationSetting.create(
            packageName = "com.example.test",
            appName = "Example Test",
            orientation = ScreenOrientation.ReversePortrait,
            targetScreen = TargetScreen.SpecificScreen.secondary(1),
            enabled = true
        )

        val entity = original.toEntity()
        val restored = entity.toDomain()

        assertEquals(original.packageName, restored.packageName)
        assertEquals(original.appName, restored.appName)
        assertEquals(original.orientation, restored.orientation)
        assertEquals(original.enabled, restored.enabled)
        // Note: TargetScreen doesn't implement equals, so compare by id
        assertEquals(original.targetScreen.id, restored.targetScreen.id)
    }

    @Test
    fun `entity contains timestamp field`() {
        val entity = AppOrientationEntity(
            packageName = "com.test",
            appName = "Test",
            orientationValue = 0,
            targetScreenId = -1,
            targetScreenName = "All",
            enabled = true,
            lastModified = 123456789L
        )

        assertEquals(123456789L, entity.lastModified)
    }
}
