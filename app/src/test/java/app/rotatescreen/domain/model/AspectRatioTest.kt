package app.rotatescreen.domain.model

import org.junit.Assert.*
import org.junit.Test

class AspectRatioTest {

    @Test
    fun `AspectRatio enum has correct values`() {
        assertEquals(3, AspectRatio.values().size)
        assertTrue(AspectRatio.values().contains(AspectRatio.PORTRAIT))
        assertTrue(AspectRatio.values().contains(AspectRatio.LANDSCAPE))
        assertTrue(AspectRatio.values().contains(AspectRatio.SQUARE))
    }

    @Test
    fun `AspectRatio PORTRAIT is distinct`() {
        val portrait = AspectRatio.PORTRAIT
        assertNotEquals(portrait, AspectRatio.LANDSCAPE)
        assertNotEquals(portrait, AspectRatio.SQUARE)
    }

    @Test
    fun `AspectRatio LANDSCAPE is distinct`() {
        val landscape = AspectRatio.LANDSCAPE
        assertNotEquals(landscape, AspectRatio.PORTRAIT)
        assertNotEquals(landscape, AspectRatio.SQUARE)
    }

    @Test
    fun `AspectRatio SQUARE is distinct`() {
        val square = AspectRatio.SQUARE
        assertNotEquals(square, AspectRatio.PORTRAIT)
        assertNotEquals(square, AspectRatio.LANDSCAPE)
    }

    @Test
    fun `AspectRatio can be used in when expressions`() {
        val testRatio = AspectRatio.PORTRAIT
        val result = when (testRatio) {
            AspectRatio.PORTRAIT -> "tall"
            AspectRatio.LANDSCAPE -> "wide"
            AspectRatio.SQUARE -> "equal"
        }
        assertEquals("tall", result)
    }

    @Test
    fun `AspectRatio LANDSCAPE when expression`() {
        val testRatio = AspectRatio.LANDSCAPE
        val result = when (testRatio) {
            AspectRatio.PORTRAIT -> "tall"
            AspectRatio.LANDSCAPE -> "wide"
            AspectRatio.SQUARE -> "equal"
        }
        assertEquals("wide", result)
    }

    @Test
    fun `AspectRatio SQUARE when expression`() {
        val testRatio = AspectRatio.SQUARE
        val result = when (testRatio) {
            AspectRatio.PORTRAIT -> "tall"
            AspectRatio.LANDSCAPE -> "wide"
            AspectRatio.SQUARE -> "equal"
        }
        assertEquals("equal", result)
    }

    @Test
    fun `AspectRatio valueOf works correctly`() {
        assertEquals(AspectRatio.PORTRAIT, AspectRatio.valueOf("PORTRAIT"))
        assertEquals(AspectRatio.LANDSCAPE, AspectRatio.valueOf("LANDSCAPE"))
        assertEquals(AspectRatio.SQUARE, AspectRatio.valueOf("SQUARE"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AspectRatio valueOf throws for invalid value`() {
        AspectRatio.valueOf("INVALID")
    }
}
