package app.rotatescreen.domain.model

import android.content.pm.ActivityInfo
import arrow.core.Either
import org.junit.Assert.*
import org.junit.Test

class ScreenOrientationTest {

    @Test
    fun `fromValue returns correct orientation for valid values`() {
        val result = ScreenOrientation.fromValue(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        assertTrue(result.isRight())
        assertEquals(ScreenOrientation.Portrait, result.getOrNull())
    }

    @Test
    fun `fromValue returns error for invalid values`() {
        val result = ScreenOrientation.fromValue(9999)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is OrientationError.InvalidOrientation)
    }

    @Test
    fun `next cycles through all orientations correctly`() {
        val sequence = listOf(
            ScreenOrientation.Unspecified to ScreenOrientation.Portrait,
            ScreenOrientation.Portrait to ScreenOrientation.Landscape,
            ScreenOrientation.Landscape to ScreenOrientation.ReversePortrait,
            ScreenOrientation.ReversePortrait to ScreenOrientation.ReverseLandscape,
            ScreenOrientation.ReverseLandscape to ScreenOrientation.Sensor,
            ScreenOrientation.Sensor to ScreenOrientation.Unspecified
        )

        sequence.forEach { (current, expected) ->
            assertEquals(expected, current.next())
        }
    }

    @Test
    fun `all returns complete list of orientations`() {
        val all = ScreenOrientation.all()
        assertEquals(6, all.size)
        assertTrue(all.contains(ScreenOrientation.Unspecified))
        assertTrue(all.contains(ScreenOrientation.Portrait))
        assertTrue(all.contains(ScreenOrientation.Landscape))
        assertTrue(all.contains(ScreenOrientation.ReversePortrait))
        assertTrue(all.contains(ScreenOrientation.ReverseLandscape))
        assertTrue(all.contains(ScreenOrientation.Sensor))
    }

    @Test
    fun `orientation value matches ActivityInfo constants`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, ScreenOrientation.Unspecified.value)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ScreenOrientation.Portrait.value)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ScreenOrientation.Landscape.value)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, ScreenOrientation.ReversePortrait.value)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, ScreenOrientation.ReverseLandscape.value)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, ScreenOrientation.Sensor.value)
    }

    @Test
    fun `orientation has correct display names`() {
        assertEquals("Auto Rotate", ScreenOrientation.Unspecified.displayName)
        assertEquals("Portrait", ScreenOrientation.Portrait.displayName)
        assertEquals("Landscape", ScreenOrientation.Landscape.displayName)
        assertEquals("Reverse Portrait", ScreenOrientation.ReversePortrait.displayName)
        assertEquals("Reverse Landscape", ScreenOrientation.ReverseLandscape.displayName)
        assertEquals("Sensor", ScreenOrientation.Sensor.displayName)
    }
}
