package com.example.data

import com.example.data.local.GameEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SudokuDao
import kotlinx.coroutines.flow.Flow

class SudokuRepository(private val dao: SudokuDao) {

    val activeGame: Flow<GameEntity?> = dao.getActiveGame()
    val history: Flow<List<HistoryEntity>> = dao.getHistory()

    suspend fun saveActiveGame(game: GameEntity) {
        dao.saveActiveGame(game)
    }

    suspend fun clearActiveGame() {
        dao.clearActiveGame()
    }

    suspend fun recordCompletedGame(
        difficulty: String,
        durationSeconds: Long,
        mistakes: Int,
        hintsUsed: Int
    ) {
        val history = HistoryEntity(
            difficulty = difficulty,
            durationSeconds = durationSeconds,
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            completedAt = System.currentTimeMillis()
        )
        dao.insertHistory(history)
        dao.clearActiveGame()
    }

    suspend fun clearAllHistory() {
        dao.clearHistory()
    }

    companion object {
        fun serializeNotes(notesList: List<Set<Int>>): String {
            return notesList.joinToString(";") { set ->
                set.sorted().joinToString(",")
            }
        }

        fun deserializeNotes(serialized: String): List<Set<Int>> {
            if (serialized.isEmpty()) return List(81) { emptySet() }
            val parts = serialized.split(";")
            return List(81) { i ->
                if (i < parts.size && parts[i].isNotEmpty()) {
                    parts[i].split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..9 }
                        .toSet()
                } else {
                    emptySet()
                }
            }
        }
    }
}
