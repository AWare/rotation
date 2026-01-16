package com.aware.rotation.data.local.dao

import androidx.room.*
import com.aware.rotation.data.local.entity.AppOrientationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOrientationDao {
    @Query("SELECT * FROM app_orientations WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<AppOrientationEntity>>

    @Query("SELECT * FROM app_orientations")
    fun getAll(): Flow<List<AppOrientationEntity>>

    @Query("SELECT * FROM app_orientations WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppOrientationEntity?

    @Query("SELECT * FROM app_orientations WHERE packageName = :packageName")
    fun observeByPackageName(packageName: String): Flow<AppOrientationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppOrientationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AppOrientationEntity>)

    @Update
    suspend fun update(entity: AppOrientationEntity)

    @Delete
    suspend fun delete(entity: AppOrientationEntity)

    @Query("DELETE FROM app_orientations WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM app_orientations")
    suspend fun deleteAll()

    @Query("UPDATE app_orientations SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}
