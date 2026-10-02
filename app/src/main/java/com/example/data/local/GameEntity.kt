package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_game")
data class GameEntity(
    @PrimaryKey val id: Int = 1,
    val puzzleClues: String,
    val currentBoard: String,
    val solution: String,
    val notesSerialized: String,
    val difficulty: String,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val isCompleted: Boolean,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
