package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val difficulty: String,
    val durationSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAt: Long = System.currentTimeMillis()
)
