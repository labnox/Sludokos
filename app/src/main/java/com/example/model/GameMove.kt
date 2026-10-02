package com.example.model

data class GameMove(
    val row: Int,
    val col: Int,
    val previousValue: Int,
    val newValue: Int,
    val previousNotes: Set<Int>,
    val newNotes: Set<Int>
)
