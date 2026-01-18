package app.rotatescreen.domain.model

import org.junit.Assert.*
import org.junit.Test

class TargetScreenTest {

    @Test
    fun `fromId with -1 returns AllScreens`() {
        val result = TargetScreen.fromId(-1)
        assertTrue(result.isRight())
        assertEquals(TargetScreen.AllScreens, result.getOrNull())
    }

    @Test
    fun `fromId with 0 returns SpecificScreen`() {
        val result = TargetScreen.fromId(0, "Primary")
        assertTrue(result.isRight())
        val screen = result.getOrNull()
        assertTrue(screen is TargetScreen.SpecificScreen)
        assertEquals(0, screen?.id)
    }

    @Test
    fun `fromId with positive id returns SpecificScreen`() {
        val result = TargetScreen.fromId(5, "Screen 5")
        assertTrue(result.isRight())
        val screen = result.getOrNull()
        assertTrue(screen is TargetScreen.SpecificScreen)
        assertEquals(5, screen?.id)
        assertEquals("Screen 5", screen?.displayName)
    }

    @Test
    fun `fromId with negative id other than -1 returns error`() {
        val result = TargetScreen.fromId(-5)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ScreenError.InvalidDisplayId)
    }

    @Test
    fun `fromId generates default name when not provided`() {
        val result = TargetScreen.fromId(3)
        assertTrue(result.isRight())
        assertEquals("Screen 3", result.getOrNull()?.displayName)
    }

    @Test
    fun `SpecificScreen primary returns display id 0`() {
        val primary = TargetScreen.SpecificScreen.primary()
        assertEquals(0, primary.displayId)
        assertEquals("Primary Screen", primary.name)
    }

    @Test
    fun `SpecificScreen secondary returns correct display id`() {
        val secondary = TargetScreen.SpecificScreen.secondary(2)
        assertEquals(2, secondary.displayId)
        assertEquals("Secondary Screen 2", secondary.name)
    }

    @Test
    fun `AllScreens has id -1`() {
        assertEquals(-1, TargetScreen.AllScreens.id)
        assertEquals("All Screens", TargetScreen.AllScreens.displayName)
    }

    @Test
    fun `SpecificScreen with aspect ratio portrait`() {
        val screen = TargetScreen.SpecificScreen(1, "Test", AspectRatio.PORTRAIT)
        assertEquals(AspectRatio.PORTRAIT, screen.ratio)
        assertEquals(AspectRatio.PORTRAIT, screen.aspectRatio)
    }

    @Test
    fun `SpecificScreen with aspect ratio landscape`() {
        val screen = TargetScreen.SpecificScreen(1, "Test", AspectRatio.LANDSCAPE)
        assertEquals(AspectRatio.LANDSCAPE, screen.ratio)
        assertEquals(AspectRatio.LANDSCAPE, screen.aspectRatio)
    }

    @Test
    fun `SpecificScreen with aspect ratio square`() {
        val screen = TargetScreen.SpecificScreen(1, "Test", AspectRatio.SQUARE)
        assertEquals(AspectRatio.SQUARE, screen.ratio)
        assertEquals(AspectRatio.SQUARE, screen.aspectRatio)
    }

    @Test
    fun `SpecificScreen defaults to landscape aspect ratio`() {
        val screen = TargetScreen.SpecificScreen(1, "Test")
        assertEquals(AspectRatio.LANDSCAPE, screen.ratio)
    }

    @Test
    fun `fromId with aspect ratio creates SpecificScreen with correct ratio`() {
        val result = TargetScreen.fromId(2, "Display", AspectRatio.PORTRAIT)
        assertTrue(result.isRight())
        val screen = result.getOrNull() as? TargetScreen.SpecificScreen
        assertNotNull(screen)
        assertEquals(AspectRatio.PORTRAIT, screen?.ratio)
    }

    @Test
    fun `AllScreens has square aspect ratio`() {
        assertEquals(AspectRatio.SQUARE, TargetScreen.AllScreens.aspectRatio)
    }
}
