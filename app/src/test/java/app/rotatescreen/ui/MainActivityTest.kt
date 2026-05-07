package app.rotatescreen.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActivityTest {

    // Mirror MainActivity.PACKAGE_NAME_REGEX for unit testing without instantiating the Activity.
    // Android package names: identifiers separated by dots; identifiers may start with any letter
    // (uppercase or lowercase) and may contain letters, digits, and underscores. At least one
    // dot is required because all real Android package names are namespaced.
    private val regex = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    private fun validate(pkg: String): String? = pkg.takeIf {
        it.isNotBlank() && it.length <= 255 && regex.matches(it)
    }

    @Test
    fun `valid lowercase package name passes validation`() {
        assertEquals("com.example.app", validate("com.example.app"))
    }

    @Test
    fun `package name with uppercase is accepted`() {
        // Real apps ship uppercase identifiers (e.g. com.UCMobile.intl). The previous
        // all-lowercase regex incorrectly rejected them.
        assertEquals("com.UCMobile.intl", validate("com.UCMobile.intl"))
    }

    @Test
    fun `empty package name is rejected`() {
        assertNull(validate(""))
    }

    @Test
    fun `blank package name is rejected`() {
        assertNull(validate("   "))
    }

    @Test
    fun `package name exceeding 255 chars is rejected`() {
        val pkg = "com." + "a".repeat(252) // length > 255
        assertNull(validate(pkg))
    }

    @Test
    fun `package name with special characters is rejected`() {
        assertNull(validate("com.example.app@test"))
    }

    @Test
    fun `package name starting with a digit is rejected`() {
        assertNull(validate("1com.example.app"))
    }

    @Test
    fun `package name with underscores is valid`() {
        assertEquals("com.example.my_app", validate("com.example.my_app"))
    }

    @Test
    fun `package name with numbers is valid`() {
        assertEquals("com.example.app123", validate("com.example.app123"))
    }

    @Test
    fun `single segment package name is rejected`() {
        // Android requires at least one dot; "app" alone is not a valid package name.
        assertNull(validate("app"))
    }

    @Test
    fun `segment starting with a digit is rejected`() {
        assertNull(validate("com.0example.app"))
    }
}
