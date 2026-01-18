package app.rotatescreen.data.local.dao

import androidx.room.*
import app.rotatescreen.data.local.entity.ScreenProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenProfileDao {
    @Query("SELECT * FROM screen_profiles ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ScreenProfileEntity>>

    @Query("SELECT * FROM screen_profiles WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<ScreenProfileEntity?>

    @Query("SELECT * FROM screen_profiles WHERE id = :id")
    suspend fun getById(id: String): ScreenProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ScreenProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ScreenProfileEntity>)

    @Update
    suspend fun update(profile: ScreenProfileEntity)

    @Delete
    suspend fun delete(profile: ScreenProfileEntity)

    @Query("DELETE FROM screen_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE screen_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE screen_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: String)

    @Query("DELETE FROM screen_profiles")
    suspend fun deleteAll()
}
