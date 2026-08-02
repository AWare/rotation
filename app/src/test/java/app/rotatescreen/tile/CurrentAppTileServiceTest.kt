package app.rotatescreen.tile

import android.provider.Settings
import android.service.quicksettings.Tile
import app.rotatescreen.data.local.RotationDatabase
import app.rotatescreen.data.local.dao.AppOrientationDao
import app.rotatescreen.domain.model.ScreenOrientation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for CurrentAppTileService.
 *
 * Robolectric's ShadowTileService supplies a real Tile, so these assert the
 * tile state the service actually produces rather than verifying calls against
 * a mocked Tile (getQsTile is final and cannot be stubbed).
 *
 * Usage-stats permission is absent in the Robolectric environment, which is the
 * branch these tests exercise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CurrentAppTileServiceTest {

    private lateinit var service: CurrentAppTileService
    private lateinit var database: RotationDatabase
    private lateinit var dao: AppOrientationDao

    @Before
    fun setup() {
        database = mockk(relaxed = true)
        dao = mockk(relaxed = true)

        // getInstance is a companion function, so mockkObject (not mockkStatic).
        mockkObject(RotationDatabase.Companion)
        every { RotationDatabase.getInstance(any()) } returns database
        every { database.appOrientationDao() } returns dao
        every { dao.getAll() } returns flowOf(emptyList())

        service = Robolectric.setupService(CurrentAppTileService::class.java)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private val tile: Tile
        get() = requireNotNull(service.qsTile) { "ShadowTileService should provide a Tile" }

    /** Reads a private field; the tile's collaborators are not otherwise exposed. */
    private fun readField(name: String): Any? =
        CurrentAppTileService::class.java
            .getDeclaredField(name)
            .apply { isAccessible = true }
            .get(service)

    @Test
    fun `onCreate builds the repository from the database`() {
        assertNotNull(service)
        verify { RotationDatabase.getInstance(any()) }
        verify { database.appOrientationDao() }
    }

    @Test
    fun `onStartListening marks the tile inactive without usage access`() {
        service.onStartListening()

        assertEquals(Tile.STATE_INACTIVE, tile.state)
        assertEquals("Current App", tile.label)
    }

    @Test
    fun `onStartListening explains that usage access is needed`() {
        service.onStartListening()

        assertEquals("Tap to grant Usage Access permission", tile.contentDescription)
    }

    @Test
    fun `onClick opens usage access settings when permission is missing`() {
        service.onClick()

        val started = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, started.action)
    }

    // onDestroy itself is not exercised here: it calls super.onDestroy(), and
    // Robolectric's ShadowTileService does not extend ShadowService (still true
    // in 4.13), so the shadow lookup throws ClassCastException before reaching
    // the service's own cleanup. The setup half is covered instead.

    @Test
    fun `onCreate leaves the scope and repository ready for use`() {
        assertNotNull(readField("serviceScope"))
        assertNotNull(readField("repository"))
    }

    @Test
    fun `orientation cycle covers every orientation in order`() {
        @Suppress("UNCHECKED_CAST")
        val cycle = readField("orientationCycle") as List<ScreenOrientation>

        assertEquals(
            listOf(
                ScreenOrientation.Unspecified,
                ScreenOrientation.Portrait,
                ScreenOrientation.Landscape,
                ScreenOrientation.Sensor,
                ScreenOrientation.ReversePortrait,
                ScreenOrientation.ReverseLandscape
            ),
            cycle
        )
    }

    @Test
    fun `orientation cycle has no duplicates`() {
        @Suppress("UNCHECKED_CAST")
        val cycle = readField("orientationCycle") as List<ScreenOrientation>

        assertTrue(cycle.isNotEmpty())
        assertEquals(cycle.size, cycle.toSet().size)
    }
}
