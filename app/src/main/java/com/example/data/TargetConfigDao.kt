package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.TargetConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetConfigDao {
    @Query("SELECT * FROM target_configs ORDER BY id ASC")
    fun getAllTargetsFlow(): Flow<List<TargetConfigEntity>>

    @Query("SELECT * FROM target_configs WHERE isEnabled = 1")
    suspend fun getActiveTargets(): List<TargetConfigEntity>

    @Query("SELECT * FROM target_configs WHERE targetType = :type AND isEnabled = 1")
    fun getActiveTargetsByTypeFlow(type: String): Flow<List<TargetConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: TargetConfigEntity): Long

    @Update
    suspend fun update(config: TargetConfigEntity)

    @Query("DELETE FROM target_configs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM target_configs")
    suspend fun count(): Int
}
