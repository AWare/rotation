package app.rotatescreen.ui

import android.content.Intent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActivityTest {

    @Test
    fun `valid package name passes validation`() {
        // Arrange
        val packageName = "com.example.app"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertEquals("com.example.app", result)
    }

    @Test
    fun `package name with uppercase is rejected`() {
        // Arrange
        val packageName = "com.Example.App"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `empty package name is rejected`() {
        // Arrange
        val packageName = ""

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `blank package name is rejected`() {
        // Arrange
        val packageName = "   "

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `package name exceeding 255 chars is rejected`() {
        // Arrange
        val packageName = "com." + "a".repeat(252) // Total > 255

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `package name with special characters is rejected`() {
        // Arrange
        val packageName = "com.example.app@test"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `package name starting with number is rejected`() {
        // Arrange
        val packageName = "1com.example.app"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertNull(result)
    }

    @Test
    fun `package name with underscores is valid`() {
        // Arrange
        val packageName = "com.example.my_app"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertEquals("com.example.my_app", result)
    }

    @Test
    fun `package name with numbers is valid`() {
        // Arrange
        val packageName = "com.example.app123"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertEquals("com.example.app123", result)
    }

    @Test
    fun `single segment package name is valid`() {
        // Arrange
        val packageName = "app"

        // Act
        val result = packageName.takeIf { pkg ->
            pkg.isNotBlank() &&
            pkg.length <= 255 &&
            pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        }

        // Assert
        assertEquals("app", result)
    }
}
