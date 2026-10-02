package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SudokuDao {
    @Query("SELECT * FROM active_game WHERE id = 1 LIMIT 1")
    fun getActiveGame(): Flow<GameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveGame(game: GameEntity)

    @Query("DELETE FROM active_game WHERE id = 1")
    suspend fun clearActiveGame()

    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM game_history ORDER BY completedAt DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM game_history")
    suspend fun clearHistory()
}
