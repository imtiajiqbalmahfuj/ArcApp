package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {
    @Query("SELECT * FROM ActionTable ORDER BY createdAt ASC")
    fun getAllActionsFlow(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM ActionTable ORDER BY createdAt ASC")
    suspend fun getAllActions(): List<ActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<ActionEntity>)

    @Update
    suspend fun updateAction(action: ActionEntity)

    @Delete
    suspend fun deleteAction(action: ActionEntity)

    @Query("DELETE FROM ActionTable WHERE id = :id")
    suspend fun deleteActionById(id: String)
}
