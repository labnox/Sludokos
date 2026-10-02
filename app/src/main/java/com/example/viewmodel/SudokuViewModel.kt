package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SudokuRepository
import com.example.data.local.GameEntity
import com.example.data.local.SudokuDatabase
import com.example.engine.SudokuEngine
import com.example.model.GameMove
import com.example.model.SudokuCell
import com.example.model.SudokuDifficulty
import com.example.model.SudokuSettings
import com.example.model.SudokuStats
import com.example.util.SoundEffectManager
import com.example.util.SpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SudokuUiState(
    val board: List<SudokuCell> = emptyList(),
    val solution: Array<IntArray> = Array(9) { IntArray(9) },
    val selectedRow: Int? = null,
    val selectedCol: Int? = null,
    val difficulty: SudokuDifficulty = SudokuDifficulty.MEDIUM,
    val isNotesMode: Boolean = false,
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val mistakesCount: Int = 0,
    val hintsUsed: Int = 0,
    val isGameOver: Boolean = false,
    val isWon: Boolean = false,
    val settings: SudokuSettings = SudokuSettings(),
    val stats: SudokuStats = SudokuStats(),
    val digitCounts: Map<Int, Int> = (1..9).associateWith { 0 },
    val conflicts: Set<Pair<Int, Int>> = emptySet(),
    val canUndo: Boolean = false,
    val hintMessage: String? = null,
    val isLoading: Boolean = true
) {
    val selectedCell: SudokuCell?
        get() = if (selectedRow != null && selectedCol != null) {
            board.getOrNull(selectedRow * 9 + selectedCol)
        } else null

    val selectedNumber: Int?
        get() = selectedCell?.value?.takeIf { it != 0 }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SudokuUiState
        return board == other.board &&
                selectedRow == other.selectedRow &&
                selectedCol == other.selectedCol &&
                difficulty == other.difficulty &&
                isNotesMode == other.isNotesMode &&
                elapsedSeconds == other.elapsedSeconds &&
                isPaused == other.isPaused &&
                mistakesCount == other.mistakesCount &&
                isGameOver == other.isGameOver &&
                isWon == other.isWon &&
                settings == other.settings &&
                canUndo == other.canUndo &&
                hintMessage == other.hintMessage &&
                isLoading == other.isLoading
    }

    override fun hashCode(): Int {
        var result = board.hashCode()
        result = 31 * result + (selectedRow ?: 0)
        result = 31 * result + (selectedCol ?: 0)
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + isNotesMode.hashCode()
        result = 31 * result + elapsedSeconds.hashCode()
        return result
    }
}

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SudokuRepository
    private val vibrator: Vibrator?
    val speechManager = SpeechManager(application)
    val soundEffectManager = SoundEffectManager()

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private val moveHistory = ArrayDeque<GameMove>()
    private var timerJob: Job? = null
    private var puzzleCluesString: String = ""
    private var lastDigitEnterTime: Long = 0L
    private var lastEnteredCellIndex: Int = -1
    private var lastEnteredDigit: Int = -1

    init {
        speechManager.isEnabled = _uiState.value.settings.speechFeedbackEnabled
        soundEffectManager.isEnabled = _uiState.value.settings.soundEffectsEnabled
        val database = SudokuDatabase.getInstance(application)
        repository = SudokuRepository(database.sudokuDao())

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Observe game history for stats
        viewModelScope.launch {
            repository.history.collectLatest { historyList ->
                val stats = SudokuStats.fromHistory(historyList)
                _uiState.update { it.copy(stats = stats) }
            }
        }

        // Attempt restoring active game or initialize new one
        viewModelScope.launch {
            repository.activeGame.collectLatest { savedGame ->
                if (savedGame != null && !savedGame.isCompleted && _uiState.value.board.isEmpty()) {
                    restoreGame(savedGame)
                } else if (_uiState.value.board.isEmpty()) {
                    startNewGame(SudokuDifficulty.MEDIUM)
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused && !_uiState.value.isWon && !_uiState.value.isGameOver) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                    // Periodically auto-save
                    if (_uiState.value.elapsedSeconds % 10L == 0L) {
                        saveCurrentGame()
                    }
                }
            }
        }
    }

    fun startNewGame(difficulty: SudokuDifficulty) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hintMessage = null) }
            moveHistory.clear()

            // Run generation on background dispatcher
            val puzzle = withContext(Dispatchers.Default) {
                SudokuEngine.generatePuzzle(difficulty)
            }

            val cells = mutableListOf<SudokuCell>()
            val sbClues = StringBuilder(81)
            for (r in 0 until 9) {
                for (c in 0 until 9) {
                    val clueVal = puzzle.initialBoard[r][c]
                    sbClues.append(clueVal)
                    cells.add(
                        SudokuCell(
                            row = r,
                            col = c,
                            value = clueVal,
                            isGiven = clueVal != 0,
                            notes = emptySet()
                        )
                    )
                }
            }
            puzzleCluesString = sbClues.toString()

            _uiState.update {
                it.copy(
                    board = cells,
                    solution = puzzle.solution,
                    selectedRow = 4,
                    selectedCol = 4,
                    difficulty = difficulty,
                    elapsedSeconds = 0,
                    isPaused = false,
                    mistakesCount = 0,
                    hintsUsed = 0,
                    isGameOver = false,
                    isWon = false,
                    conflicts = emptySet(),
                    canUndo = false,
                    digitCounts = calculateDigitCounts(cells),
                    isLoading = false
                )
            }

            startTimer()
            saveCurrentGame()
        }
    }

    fun restartCurrentGame() {
        val state = _uiState.value
        if (puzzleCluesString.isEmpty() && state.board.isNotEmpty()) {
            puzzleCluesString = state.board.joinToString("") { if (it.isGiven) it.value.toString() else "0" }
        }
        val clues = SudokuEngine.parseGrid(puzzleCluesString)
        val cells = mutableListOf<SudokuCell>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val v = clues[r][c]
                cells.add(
                    SudokuCell(
                        row = r,
                        col = c,
                        value = v,
                        isGiven = v != 0,
                        notes = emptySet()
                    )
                )
            }
        }
        moveHistory.clear()
        _uiState.update {
            it.copy(
                board = cells,
                elapsedSeconds = 0,
                isPaused = false,
                mistakesCount = 0,
                hintsUsed = 0,
                isGameOver = false,
                isWon = false,
                conflicts = emptySet(),
                canUndo = false,
                hintMessage = null,
                digitCounts = calculateDigitCounts(cells)
            )
        }
        startTimer()
        saveCurrentGame()
    }

    private fun restoreGame(saved: GameEntity) {
        val difficulty = SudokuDifficulty.fromString(saved.difficulty)
        val currentGrid = SudokuEngine.parseGrid(saved.currentBoard)
        val cluesGrid = SudokuEngine.parseGrid(saved.puzzleClues)
        val solutionGrid = SudokuEngine.parseGrid(saved.solution)
        val notes = SudokuRepository.deserializeNotes(saved.notesSerialized)

        puzzleCluesString = saved.puzzleClues
        val cells = mutableListOf<SudokuCell>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val idx = r * 9 + c
                val value = currentGrid[r][c]
                val isGiven = cluesGrid[r][c] != 0
                cells.add(
                    SudokuCell(
                        row = r,
                        col = c,
                        value = value,
                        isGiven = isGiven,
                        notes = if (value == 0 && idx < notes.size) notes[idx] else emptySet()
                    )
                )
            }
        }

        val conflicts = if (_uiState.value.settings.highlightDuplicates) {
            SudokuEngine.findConflicts(cells)
        } else emptySet()

        _uiState.update {
            it.copy(
                board = cells,
                solution = solutionGrid,
                difficulty = difficulty,
                elapsedSeconds = saved.elapsedSeconds,
                mistakesCount = saved.mistakes,
                hintsUsed = saved.hintsUsed,
                selectedRow = 4,
                selectedCol = 4,
                isPaused = false,
                isGameOver = false,
                isWon = false,
                conflicts = conflicts,
                digitCounts = calculateDigitCounts(cells),
                isLoading = false
            )
        }
        startTimer()
    }

    fun selectCell(row: Int, col: Int, announce: Boolean = false) {
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isWon) return
        speechManager.stop()
        _uiState.update { it.copy(selectedRow = row, selectedCol = col, hintMessage = null) }
        if (announce) {
            announceCell(row, col)
        }
    }

    /**
     * Called when the user touches or drags their finger across the Sudoku grid.
     * Immediately terminates any ongoing speech and announces the cell under the finger.
     * If moving between cells, announces the direction (Omhoog, Omlaag, Links, Rechts, Kruislings).
     */
    fun onCellTouch(row: Int, col: Int, isInitialDown: Boolean) {
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isWon) return
        val prevR = _uiState.value.selectedRow
        val prevC = _uiState.value.selectedCol

        if (!isInitialDown && prevR == row && prevC == col) {
            return // Same cell while dragging, do not repeat
        }

        // Determine navigation direction
        val direction = if (!isInitialDown && prevR != null && prevC != null) {
            when {
                row != prevR && col != prevC -> "Kruislings"
                row < prevR -> "Omhoog"
                row > prevR -> "Omlaag"
                col < prevC -> "Links"
                col > prevC -> "Rechts"
                else -> ""
            }
        } else ""

        speechManager.stop()
        soundEffectManager.playCellSelect()
        triggerHaptic()
        _uiState.update { it.copy(selectedRow = row, selectedCol = col, hintMessage = null) }
        announceCell(row, col, prefix = direction)
    }

    /**
     * Directional step-by-step navigation (e.g. Omhoog, Omlaag, Links, Rechts)
     */
    fun navigateDirection(dRow: Int, dCol: Int, directionName: String) {
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isWon) return
        val currR = _uiState.value.selectedRow ?: 4
        val currC = _uiState.value.selectedCol ?: 4
        val newR = (currR + dRow).coerceIn(0, 8)
        val newC = (currC + dCol).coerceIn(0, 8)
        speechManager.stop()
        soundEffectManager.playCellSelect()
        triggerHaptic()
        _uiState.update { it.copy(selectedRow = newR, selectedCol = newC, hintMessage = null) }
        announceCell(newR, newC, prefix = directionName)
    }

    /**
     * Kruislings (diagonal) navigation across the board.
     */
    fun navigateKruislings() {
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isWon) return
        val currR = _uiState.value.selectedRow ?: 4
        val currC = _uiState.value.selectedCol ?: 4
        val newR = if (currR < 8) currR + 1 else 0
        val newC = if (currC < 8) currC + 1 else 0
        speechManager.stop()
        soundEffectManager.playCellSelect()
        triggerHaptic()
        _uiState.update { it.copy(selectedRow = newR, selectedCol = newC, hintMessage = null) }
        announceCell(newR, newC, prefix = "Kruislings")
    }

    fun repeatCurrentCellAnnouncement() {
        val r = _uiState.value.selectedRow ?: 4
        val c = _uiState.value.selectedCol ?: 4
        speechManager.stop()
        val index = r * 9 + c
        val cell = _uiState.value.board.getOrNull(index) ?: return
        val cellContent = when {
            cell.isGiven -> "${cell.value}, vast getal"
            cell.value != 0 -> "${cell.value}"
            cell.notes.isNotEmpty() -> "Geel potlood getal ${cell.notes.sorted().joinToString(", ")}"
            else -> "leeg"
        }
        speechManager.speak("Rij ${r + 1}, kolom ${c + 1}: $cellContent", interrupt = true)
    }

    fun announceCell(row: Int, col: Int, prefix: String = "") {
        if (!_uiState.value.settings.speechFeedbackEnabled) return
        val index = row * 9 + col
        val cell = _uiState.value.board.getOrNull(index) ?: return
        val text = buildString {
            if (_uiState.value.settings.speechAnnounceCoordinates) {
                if (prefix.isNotBlank()) {
                    append("$prefix, ")
                }
                append("Rij ${row + 1}, kolom ${col + 1}: ")
            }
            when {
                cell.isGiven -> append("${cell.value}, vast getal")
                cell.value != 0 -> {
                    append("${cell.value}")
                    if (cell.isHinted) append(", hint")
                    if (_uiState.value.conflicts.contains(row to col)) append(", conflict")
                }
                cell.notes.isNotEmpty() -> append("Geel potlood getal ${cell.notes.sorted().joinToString(", ")}")
                else -> append("leeg")
            }
        }
        speechManager.stop()
        speechManager.speak(text, interrupt = true)
    }

    fun onDigitHover(digit: Int) {
        soundEffectManager.playNumberSelect(digit)
        if (!_uiState.value.settings.speechFeedbackEnabled) return
        speechManager.stop()
        triggerHaptic()
        val count = _uiState.value.digitCounts[digit] ?: 0
        val remaining = (9 - count).coerceAtLeast(0)
        val text = if (remaining == 0) {
            "Cijfer $digit, compleet"
        } else {
            "Cijfer $digit, $remaining nodig"
        }
        speechManager.speak(text, interrupt = true)
    }

    fun announceKeypad(digit: Int) {
        onDigitHover(digit)
    }

    fun announceAction(label: String) {
        if (!_uiState.value.settings.speechFeedbackEnabled) return
        speechManager.stop()
        speechManager.speak(label, interrupt = true)
    }

    fun toggleSpeechFeedback() {
        val newSetting = !_uiState.value.settings.speechFeedbackEnabled
        val newSettings = _uiState.value.settings.copy(speechFeedbackEnabled = newSetting)
        updateSettings(newSettings)
        speechManager.isEnabled = newSetting
        speechManager.stop()
        if (newSetting) {
            speechManager.speak("Spraak ingeschakeld", interrupt = true)
        }
    }

    fun toggleNotesMode() {
        triggerHaptic()
        speechManager.stop()
        val newMode = !_uiState.value.isNotesMode
        _uiState.update { it.copy(isNotesMode = newMode) }
        if (_uiState.value.settings.speechFeedbackEnabled) {
            speechManager.speak(if (newMode) "Notitiemodus ingeschakeld" else "Notitiemodus uitgeschakeld", interrupt = true)
        }
    }

    fun enterDigit(digit: Int) {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isWon) return
        speechManager.stop()
        triggerHaptic()

        val r = state.selectedRow
        val c = state.selectedCol
        if (r == null || c == null) {
            val count = state.digitCounts[digit] ?: 0
            val remaining = (9 - count).coerceAtLeast(0)
            val msg = if (remaining == 0) {
                "Cijfer $digit, compleet. Kies eerst een cel op het bord."
            } else {
                "Cijfer $digit, $remaining over. Kies eerst een cel op het bord."
            }
            speechManager.speak(msg, interrupt = true)
            return
        }

        val index = r * 9 + c
        val cell = state.board[index]

        // Given clues cannot be modified
        if (cell.isGiven) {
            soundEffectManager.playErrorAlert()
            val loc = if (state.settings.speechAnnounceCoordinates) " op rij ${r + 1}, kolom ${c + 1}" else ""
            speechManager.speak("Vast getal ${cell.value}$loc, kan niet gewijzigd worden.", interrupt = true)
            return
        }

        if (state.isNotesMode) {
            // Notes Mode: Toggle candidate number in notes
            val currentNotes = cell.notes
            val isRemoved = currentNotes.contains(digit)
            if (isRemoved) {
                soundEffectManager.playErase()
            } else {
                soundEffectManager.playNumberSelect(digit)
            }
            val newNotes = if (isRemoved) {
                currentNotes - digit
            } else {
                currentNotes + digit
            }
            recordMove(r, c, cell.value, cell.value, currentNotes, newNotes)

            val updatedBoard = state.board.toMutableList()
            updatedBoard[index] = cell.copy(notes = newNotes, value = 0)
            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    canUndo = moveHistory.isNotEmpty()
                )
            }
            if (state.settings.speechFeedbackEnabled) {
                val loc = if (state.settings.speechAnnounceCoordinates) " op rij ${r + 1}, kolom ${c + 1}" else ""
                val msg = if (isRemoved) "Notitie $digit gewist$loc" else "Geel potlood getal $digit toegevoegd$loc"
                speechManager.speak(msg, interrupt = true)
            }
        } else {
            // Normal Number Placement
            val oldValue = cell.value
            val now = System.currentTimeMillis()
            val isQuickDoubleTap = (index == lastEnteredCellIndex && digit == lastEnteredDigit && (now - lastDigitEnterTime) < 650L)

            val newValue = if (isQuickDoubleTap) {
                digit // Keep digit placed, never clear on rapid double-tap
            } else if (oldValue == digit) {
                0 // tapping same number after delay clears it
            } else {
                digit
            }

            lastDigitEnterTime = now
            lastEnteredCellIndex = index
            lastEnteredDigit = digit

            val oldNotes = cell.notes
            val newNotes = emptySet<Int>()

            recordMove(r, c, oldValue, newValue, oldNotes, newNotes)

            val updatedBoard = state.board.toMutableList()
            updatedBoard[index] = cell.copy(value = newValue, notes = newNotes)

            // Auto-remove notes from peers if enabled
            if (state.settings.autoRemoveNotes && newValue != 0) {
                removeNotesFromPeers(updatedBoard, r, c, newValue)
            }

            // Check correctness against solution
            val isWrong = newValue != 0 && newValue != state.solution[r][c]
            var newMistakes = state.mistakesCount
            var isGameOver = false
            if (isWrong) {
                newMistakes++
                triggerHapticError()
                if (state.settings.maxMistakesEnabled && newMistakes >= state.settings.maxMistakes) {
                    isGameOver = true
                }
            }

            // Calculate conflicts if duplicate highlights enabled
            val conflicts = if (state.settings.highlightDuplicates) {
                SudokuEngine.findConflicts(updatedBoard)
            } else emptySet()

            val digitCounts = calculateDigitCounts(updatedBoard)

            // Check for victory
            val isWon = checkVictory(updatedBoard, state.solution)
            if (isWon) {
                soundEffectManager.playPuzzleSuccess()
                triggerHapticVictory()
                onGameWon(newMistakes)
            } else if (isWrong || conflicts.contains(r to c)) {
                soundEffectManager.playErrorAlert()
            } else if (newValue != 0) {
                soundEffectManager.playNumberSelect(digit)
            } else {
                soundEffectManager.playErase()
            }

            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    conflicts = conflicts,
                    digitCounts = digitCounts,
                    mistakesCount = newMistakes,
                    isGameOver = isGameOver,
                    isWon = isWon,
                    canUndo = moveHistory.isNotEmpty()
                )
            }

            if (state.settings.speechFeedbackEnabled) {
                val loc = if (state.settings.speechAnnounceCoordinates) " op rij ${r + 1}, kolom ${c + 1}" else ""
                val msg = when {
                    isWon -> "Gefeliciteerd! Puzzel compleet opgelost!"
                    newValue == 0 -> "Cijfer gewist$loc"
                    isWrong -> "Fout! Cijfer $digit$loc"
                    else -> "Cijfer $digit geplaatst$loc"
                }
                speechManager.speak(msg, interrupt = true)
            }

            saveCurrentGame()
        }
    }

    fun eraseCell() {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isWon) return
        val r = state.selectedRow ?: return
        val c = state.selectedCol ?: return
        val index = r * 9 + c
        val cell = state.board[index]

        if (cell.isGiven) {
            soundEffectManager.playErrorAlert()
            return
        }
        if (cell.value == 0 && cell.notes.isEmpty()) return

        soundEffectManager.playErase()
        triggerHaptic()
        recordMove(r, c, cell.value, 0, cell.notes, emptySet())

        val updatedBoard = state.board.toMutableList()
        updatedBoard[index] = cell.copy(value = 0, notes = emptySet())

        val conflicts = if (state.settings.highlightDuplicates) {
            SudokuEngine.findConflicts(updatedBoard)
        } else emptySet()

        val digitCounts = calculateDigitCounts(updatedBoard)

        _uiState.update {
            it.copy(
                board = updatedBoard,
                conflicts = conflicts,
                digitCounts = digitCounts,
                canUndo = moveHistory.isNotEmpty()
            )
        }
        if (state.settings.speechFeedbackEnabled) {
            speechManager.speak("Cel op rij ${r + 1}, kolom ${c + 1} leeggemaakt", interrupt = true)
        }
        saveCurrentGame()
    }

    fun undo() {
        if (moveHistory.isEmpty()) return
        val move = moveHistory.removeLast()
        val state = _uiState.value
        val index = move.row * 9 + move.col
        val cell = state.board[index]

        triggerHaptic()

        val updatedBoard = state.board.toMutableList()
        updatedBoard[index] = cell.copy(
            value = move.previousValue,
            notes = move.previousNotes
        )

        val conflicts = if (state.settings.highlightDuplicates) {
            SudokuEngine.findConflicts(updatedBoard)
        } else emptySet()

        val digitCounts = calculateDigitCounts(updatedBoard)

        _uiState.update {
            it.copy(
                board = updatedBoard,
                selectedRow = move.row,
                selectedCol = move.col,
                conflicts = conflicts,
                digitCounts = digitCounts,
                canUndo = moveHistory.isNotEmpty(),
                isGameOver = false
            )
        }
        if (state.settings.speechFeedbackEnabled) {
            speechManager.speak("Vorige zet hersteld", interrupt = true)
        }
        saveCurrentGame()
    }

    private fun recordMove(
        r: Int,
        c: Int,
        prevVal: Int,
        newVal: Int,
        prevNotes: Set<Int>,
        newNotes: Set<Int>
    ) {
        moveHistory.addLast(
            GameMove(
                row = r,
                col = c,
                previousValue = prevVal,
                newValue = newVal,
                previousNotes = prevNotes,
                newNotes = newNotes
            )
        )
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isWon) return

        triggerHaptic()
        val hint = SudokuEngine.getSmartHint(state.board, state.solution)
        if (hint != null) {
            val r = hint.row
            val c = hint.col
            val index = r * 9 + c
            val cell = state.board[index]

            recordMove(r, c, cell.value, hint.value, cell.notes, emptySet())

            val updatedBoard = state.board.toMutableList()
            updatedBoard[index] = cell.copy(
                value = hint.value,
                notes = emptySet(),
                isHinted = true
            )

            if (state.settings.autoRemoveNotes) {
                removeNotesFromPeers(updatedBoard, r, c, hint.value)
            }

            val conflicts = if (state.settings.highlightDuplicates) {
                SudokuEngine.findConflicts(updatedBoard)
            } else emptySet()

            val digitCounts = calculateDigitCounts(updatedBoard)
            val isWon = checkVictory(updatedBoard, state.solution)
            if (isWon) {
                triggerHapticVictory()
                onGameWon(state.mistakesCount)
            }

            _uiState.update {
                it.copy(
                    board = updatedBoard,
                    selectedRow = r,
                    selectedCol = c,
                    hintsUsed = it.hintsUsed + 1,
                    hintMessage = hint.explanation,
                    conflicts = conflicts,
                    digitCounts = digitCounts,
                    isWon = isWon,
                    canUndo = moveHistory.isNotEmpty()
                )
            }
            if (state.settings.speechFeedbackEnabled) {
                speechManager.speak("Hint: ${hint.explanation}", interrupt = true)
            }
            saveCurrentGame()
        }
    }

    fun dismissHint() {
        _uiState.update { it.copy(hintMessage = null) }
    }

    fun autoFillAllNotes() {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isWon) return

        triggerHaptic()
        val candidates = SudokuEngine.computePossibleCandidates(state.board)
        val updatedBoard = state.board.mapIndexed { idx, cell ->
            if (cell.isEmpty && !cell.isGiven) {
                val r = idx / 9
                val c = idx % 9
                cell.copy(notes = candidates[r to c] ?: emptySet())
            } else cell
        }

        _uiState.update { it.copy(board = updatedBoard) }
        if (state.settings.speechFeedbackEnabled) {
            speechManager.speak("Alle mogelijke notities ingevuld", interrupt = true)
        }
        saveCurrentGame()
    }

    fun clearAllNotes() {
        val state = _uiState.value
        triggerHaptic()
        val updatedBoard = state.board.map { cell ->
            if (cell.notes.isNotEmpty()) cell.copy(notes = emptySet()) else cell
        }
        _uiState.update { it.copy(board = updatedBoard) }
        if (state.settings.speechFeedbackEnabled) {
            speechManager.speak("Alle notities gewist", interrupt = true)
        }
        saveCurrentGame()
    }

    fun togglePause() {
        triggerHaptic()
        val isNowPaused = !_uiState.value.isPaused
        _uiState.update { it.copy(isPaused = isNowPaused) }
        if (_uiState.value.settings.speechFeedbackEnabled) {
            speechManager.speak(if (isNowPaused) "Spel gepauzeerd" else "Spel hervat", interrupt = true)
        }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
        if (_uiState.value.settings.speechFeedbackEnabled) {
            speechManager.speak("Spel hervat", interrupt = true)
        }
    }

    fun updateSettings(newSettings: SudokuSettings) {
        val state = _uiState.value
        speechManager.isEnabled = newSettings.speechFeedbackEnabled
        soundEffectManager.isEnabled = newSettings.soundEffectsEnabled
        val conflicts = if (newSettings.highlightDuplicates) {
            SudokuEngine.findConflicts(state.board)
        } else emptySet()

        _uiState.update {
            it.copy(
                settings = newSettings,
                conflicts = conflicts
            )
        }
    }

    fun dismissVictoryDialog() {
        _uiState.update { it.copy(isWon = false) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
        soundEffectManager.release()
    }

    private fun removeNotesFromPeers(board: MutableList<SudokuCell>, r: Int, c: Int, num: Int) {
        // Row & Column peers
        for (i in 0 until 9) {
            val rIdx = r * 9 + i
            if (board[rIdx].notes.contains(num)) {
                board[rIdx] = board[rIdx].copy(notes = board[rIdx].notes - num)
            }
            val cIdx = i * 9 + c
            if (board[cIdx].notes.contains(num)) {
                board[cIdx] = board[cIdx].copy(notes = board[cIdx].notes - num)
            }
        }
        // 3x3 Block peers
        val startR = (r / 3) * 3
        val startC = (c / 3) * 3
        for (dr in 0 until 3) {
            for (dc in 0 until 3) {
                val bIdx = (startR + dr) * 9 + (startC + dc)
                if (board[bIdx].notes.contains(num)) {
                    board[bIdx] = board[bIdx].copy(notes = board[bIdx].notes - num)
                }
            }
        }
    }

    private fun calculateDigitCounts(board: List<SudokuCell>): Map<Int, Int> {
        val map = (1..9).associateWith { 0 }.toMutableMap()
        for (cell in board) {
            if (cell.value in 1..9) {
                map[cell.value] = (map[cell.value] ?: 0) + 1
            }
        }
        return map
    }

    private fun checkVictory(board: List<SudokuCell>, solution: Array<IntArray>): Boolean {
        if (board.any { it.value == 0 }) return false
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board[r * 9 + c].value != solution[r][c]) {
                    return false
                }
            }
        }
        return true
    }

    private fun onGameWon(mistakes: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            repository.recordCompletedGame(
                difficulty = state.difficulty.name,
                durationSeconds = state.elapsedSeconds,
                mistakes = mistakes,
                hintsUsed = state.hintsUsed
            )
        }
    }

    private fun saveCurrentGame() {
        val state = _uiState.value
        if (state.board.isEmpty() || state.isWon || state.isGameOver) return

        val currentStr = state.board.joinToString("") { it.value.toString() }
        val solutionStr = SudokuEngine.gridToString(state.solution)
        val notesList = state.board.map { it.notes }
        val notesStr = SudokuRepository.serializeNotes(notesList)

        viewModelScope.launch {
            val entity = GameEntity(
                id = 1,
                puzzleClues = puzzleCluesString,
                currentBoard = currentStr,
                solution = solutionStr,
                notesSerialized = notesStr,
                difficulty = state.difficulty.name,
                elapsedSeconds = state.elapsedSeconds,
                mistakes = state.mistakesCount,
                hintsUsed = state.hintsUsed,
                isCompleted = false
            )
            repository.saveActiveGame(entity)
        }
    }

    private fun triggerHaptic() {
        if (!_uiState.value.settings.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticError() {
        if (!_uiState.value.settings.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticVictory() {
        if (!_uiState.value.settings.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 80, 50, 100), -1)
            }
        } catch (_: Exception) {}
    }
}
