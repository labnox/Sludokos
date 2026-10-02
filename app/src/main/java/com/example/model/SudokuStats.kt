package com.example.model

import com.example.data.local.HistoryEntity

data class DifficultyStats(
    val difficulty: SudokuDifficulty,
    val gamesWon: Int,
    val bestTimeSeconds: Long?,
    val averageTimeSeconds: Long?
)

data class SudokuStats(
    val totalGamesWon: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val difficultyStats: Map<SudokuDifficulty, DifficultyStats> = emptyMap()
) {
    companion object {
        fun fromHistory(history: List<HistoryEntity>): SudokuStats {
            if (history.isEmpty()) return SudokuStats()

            val byDifficulty = mutableMapOf<SudokuDifficulty, DifficultyStats>()
            for (diff in SudokuDifficulty.entries) {
                val matches = history.filter { it.difficulty.equals(diff.name, ignoreCase = true) }
                val count = matches.size
                val best = matches.map { it.durationSeconds }.minOrNull()
                val avg = if (count > 0) matches.map { it.durationSeconds }.average().toLong() else null
                byDifficulty[diff] = DifficultyStats(
                    difficulty = diff,
                    gamesWon = count,
                    bestTimeSeconds = best,
                    averageTimeSeconds = avg
                )
            }

            return SudokuStats(
                totalGamesWon = history.size,
                currentStreak = history.size, // for simplicity
                bestStreak = history.size,
                difficultyStats = byDifficulty
            )
        }
    }
}
