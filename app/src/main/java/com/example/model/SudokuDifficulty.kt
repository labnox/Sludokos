package com.example.model

enum class SudokuDifficulty(
    val title: String,
    val cluesCount: Int,
    val description: String
) {
    EASY("Makkelijk", 40, "Ideaal voor ontspanning en beginners"),
    MEDIUM("Gemiddeld", 33, "Een gebalanceerde uitdaging met logica"),
    HARD("Moeilijk", 28, "Vereist geavanceerde technieken"),
    EXPERT("Expert", 24, "Alleen voor doorgewinterde puzzelaars");

    companion object {
        fun fromString(name: String): SudokuDifficulty {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }
    }
}
