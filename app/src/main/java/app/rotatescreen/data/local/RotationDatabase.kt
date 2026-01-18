package app.rotatescreen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.rotatescreen.data.local.dao.AppOrientationDao
import app.rotatescreen.data.local.dao.ScreenProfileDao
import app.rotatescreen.data.local.entity.AppOrientationEntity
import app.rotatescreen.data.local.entity.ScreenProfileEntity

@Database(
    entities = [AppOrientationEntity::class, ScreenProfileEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RotationDatabase : RoomDatabase() {
    abstract fun appOrientationDao(): AppOrientationDao
    abstract fun screenProfileDao(): ScreenProfileDao

    companion object {
        @Volatile
        private var INSTANCE: RotationDatabase? = null

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
            .fallbackToDestructiveMigration()
            .build()
    }
}
