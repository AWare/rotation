package com.aware.rotation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aware.rotation.data.local.dao.AppOrientationDao
import com.aware.rotation.data.local.entity.AppOrientationEntity

@Database(
    entities = [AppOrientationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RotationDatabase : RoomDatabase() {
    abstract fun appOrientationDao(): AppOrientationDao

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
