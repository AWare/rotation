package app.rotatescreen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import app.rotatescreen.data.local.dao.AppOrientationDao
import app.rotatescreen.data.local.dao.ScreenProfileDao
import app.rotatescreen.data.local.entity.AppOrientationEntity
import app.rotatescreen.data.local.entity.ScreenProfileEntity

/**
 * Schema version. Declared here rather than inline so tests can assert against
 * it: Room's @Database annotation is not retained at runtime.
 */
const val ROTATION_DB_VERSION = 3

@Database(
    entities = [AppOrientationEntity::class, ScreenProfileEntity::class],
    version = ROTATION_DB_VERSION,
    exportSchema = true
)
abstract class RotationDatabase : RoomDatabase() {
    abstract fun appOrientationDao(): AppOrientationDao
    abstract fun screenProfileDao(): ScreenProfileDao

    companion object {
        @Volatile
        private var INSTANCE: RotationDatabase? = null

        /**
         * Migrations from version 3 onward.
         *
         * Version 3 is the baseline: the applicationId changed from
         * com.aware.rotation to app.rotatescreen in the same commit that
         * introduced it, so a v1 database lives under a different package and
         * is unreachable from this app. There is nothing earlier to migrate.
         *
         * Every schema change from here needs an entry, plus a test in
         * RotationDatabaseMigrationTest.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun getInstance(context: Context): RotationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): RotationDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RotationDatabase::class.java,
                "rotation_db"
            )
            .addMigrations(*MIGRATIONS)
            // Deliberately NOT fallbackToDestructiveMigration(): that silently
            // deleted every per-app orientation setting on any schema change.
            // A missing migration should fail loudly in development instead.
            // Downgrades are only reachable by installing an older build over
            // a newer one, which Room cannot migrate backwards anyway.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}
