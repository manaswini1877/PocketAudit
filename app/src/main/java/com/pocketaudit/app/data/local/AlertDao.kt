package com.pocketaudit.app.data.local

import androidx.room.*
import com.pocketaudit.app.data.model.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    fun getAlertById(id: Long): Flow<AlertEntity?>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentAlertsDirect(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isSafe = 1 WHERE id = :id")
    suspend fun markAsSafe(id: Long)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()
}
