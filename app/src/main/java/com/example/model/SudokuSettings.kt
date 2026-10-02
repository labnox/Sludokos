package com.example.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SEPIA,
    MIDNIGHT
}

data class SudokuSettings(
    val highlightDuplicates: Boolean = true,
    val highlightCrosshair: Boolean = true,
    val highlightSameNumber: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val speechFeedbackEnabled: Boolean = true,
    val speechAnnounceCoordinates: Boolean = false,
    val showNavButtons: Boolean = false,
    val highContrast: Boolean = false,
    val maxMistakesEnabled: Boolean = false,
    val maxMistakes: Int = 3,
    val showTimer: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)
