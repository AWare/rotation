package app.rotatescreen.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the thing that used to silently delete every user setting.
 *
 * The database previously used fallbackToDestructiveMigration(), so any schema
 * change wiped all per-app orientation config with no warning. These tests fail
 * if the schema version moves without a corresponding migration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RotationDatabaseMigrationTest {

    private companion object {
        const val TEST_DB = "migration-test-db"

        /** Baseline. See RotationDatabase.MIGRATIONS for why nothing is older. */
        const val BASELINE_VERSION = 3
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RotationDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val currentVersion: Int get() = ROTATION_DB_VERSION

    @Test
    fun `baseline schema can be created`() {
        // Fails if the exported schema for the baseline is missing from
        // app/schemas, which is what makes migrations writable at all.
        helper.createDatabase(TEST_DB, BASELINE_VERSION).close()
    }

    @Test
    fun `every version bump past the baseline ships a migration`() {
        // Room applies migrations by version step, so covering the whole range
        // means one migration per version increment.
        val expected = currentVersion - BASELINE_VERSION
        assertEquals(
            "Schema is at v$currentVersion but RotationDatabase.MIGRATIONS has " +
                "${RotationDatabase.MIGRATIONS.size} entries. Add a Migration " +
                "for each version bump, or existing installs lose their settings.",
            expected,
            RotationDatabase.MIGRATIONS.size
        )
    }

    @Test
    fun `data written at the baseline survives migration to the current version`() {
        helper.createDatabase(TEST_DB, BASELINE_VERSION).use { db ->
            db.execSQL(
                """
                INSERT INTO app_orientations
                    (packageName, targetScreenId, appName, orientationValue,
                     targetScreenName, aspectRatioValue, enabled, lastModified)
                VALUES ('com.example.app', -1, 'Example', 1, 'All', 'LANDSCAPE', 1, 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            currentVersion,
            true,
            *RotationDatabase.MIGRATIONS
        ).use { db ->
            db.query("SELECT packageName, appName FROM app_orientations").use { cursor ->
                assertTrue("the row inserted before migrating is gone", cursor.moveToFirst())
                assertEquals("com.example.app", cursor.getString(0))
                assertEquals("Example", cursor.getString(1))
            }
        }
    }

    @Test
    fun `the built database opens against the current schema`() {
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RotationDatabase::class.java,
            TEST_DB
        ).addMigrations(*RotationDatabase.MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

        db.openHelper.writableDatabase.use {
            assertEquals(currentVersion, it.version)
        }
        db.close()
    }
}
