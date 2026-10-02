package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SudokuDifficulty
import com.example.ui.components.SudokuControls
import com.example.ui.components.SudokuDirectionalBar
import com.example.ui.components.SudokuGrid
import com.example.ui.components.SudokuHeader
import com.example.ui.components.SudokuKeypad
import com.example.ui.components.SudokuPositionBar
import com.example.ui.dialogs.AboutDialog
import com.example.ui.dialogs.GameOverDialog
import com.example.ui.dialogs.PwaDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.StatsDialog
import com.example.ui.dialogs.VictoryDialog
import com.example.viewmodel.SudokuViewModel

@Composable
fun SudokuMainScreen(
    viewModel: SudokuViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showPwaDialog by remember { mutableStateOf(false) }
    var showPwaWebView by remember { mutableStateOf(false) }
    var pendingDifficultyChange by remember { mutableStateOf<SudokuDifficulty?>(null) }

    if (showPwaWebView) {
        PwaWebViewScreen(onBack = { showPwaWebView = false })
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val screenHeight = maxHeight
            val screenWidth = maxWidth
            val scrollState = rememberScrollState()

            // Dynamically calculate optimal board size so Header, Position Bar,
            // Board, Nav Bar, Controls, and Keypad ALL FIT comfortably on screen!
            val reservedHeight = 280.dp
            val availableGridHeight = (screenHeight - reservedHeight).coerceAtLeast(220.dp)
            val availableGridWidth = (screenWidth - 16.dp).coerceAtLeast(220.dp)
            val calculatedBoardSize = minOf(availableGridWidth, availableGridHeight, 440.dp)

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Sudoku puzzel laden. Even geduld alstublieft."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Sudoku puzzel laden...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp)
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Header Section
                    SudokuHeader(
                        difficulty = uiState.difficulty,
                        elapsedSeconds = uiState.elapsedSeconds,
                        isPaused = uiState.isPaused,
                        mistakesCount = uiState.mistakesCount,
                        settings = uiState.settings,
                        onDifficultySelect = { diff ->
                            if (diff != uiState.difficulty) {
                                pendingDifficultyChange = diff
                            }
                        },
                        onTogglePause = { viewModel.togglePause() },
                        onRestartClick = {
                            viewModel.announceAction("Puzzel opnieuw starten")
                            showRestartDialog = true
                        },
                        onSettingsClick = {
                            viewModel.announceAction("Instellingen")
                            showSettingsDialog = true
                        },
                        onStatsClick = {
                            viewModel.announceAction("Statistieken")
                            showStatsDialog = true
                        },
                        onAboutClick = {
                            viewModel.announceAction("Uitleg van de knoppen")
                            showAboutDialog = true
                        },
                        onPwaClick = {
                            viewModel.announceAction("PWA Web App opties")
                            showPwaDialog = true
                        }
                    )

                    // Smart Educational Hint Banner
                    AnimatedVisibility(
                        visible = uiState.hintMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        uiState.hintMessage?.let { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                    .semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        contentDescription = "Hint ontvangen: $msg"
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = msg,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissHint() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Sluit hint bericht",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Position & Content Status Bar with Audio Repeat
                    SudokuPositionBar(
                        selectedCell = uiState.selectedCell,
                        onRepeatAnnouncement = { viewModel.repeatCurrentCellAnnouncement() }
                    )

                    // Sudoku 9x9 Grid Board
                    Box(contentAlignment = Alignment.Center) {
                        SudokuGrid(
                            board = uiState.board,
                            selectedRow = uiState.selectedRow,
                            selectedCol = uiState.selectedCol,
                            conflicts = uiState.conflicts,
                            settings = uiState.settings,
                            boardSize = calculatedBoardSize,
                            onCellClick = { r, c -> viewModel.selectCell(r, c, announce = true) },
                            onCellHover = { r, c -> viewModel.onCellTouch(r, c, false) },
                            onCellTouch = { r, c, isInitialDown -> viewModel.onCellTouch(r, c, isInitialDown) }
                        )

                        // Pause Privacy Overlay
                        if (uiState.isPaused) {
                            Surface(
                                modifier = Modifier
                                    .size(calculatedBoardSize)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        contentDescription = "Spel is gepauzeerd. Tik op de hervatten knop om verder te spelen."
                                    },
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Spel Gepauzeerd",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.resumeGame() },
                                        modifier = Modifier.testTag("button_resume_game")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                        )
                                        Text("Hervatten")
                                    }
                                }
                            }
                        }
                    }

                    // Directional Quick Navigation Bar (Omhoog, Omlaag, Links, Rechts, Kruislings) - hidden by default
                    if (uiState.settings.showNavButtons) {
                        SudokuDirectionalBar(
                            onNavigate = { dRow, dCol, dirName -> viewModel.navigateDirection(dRow, dCol, dirName) },
                            onNavigateKruislings = { viewModel.navigateKruislings() }
                        )
                    }

                    // Controls Row (Undo, Erase, Notes, Hint, Magic)
                    SudokuControls(
                        isNotesMode = uiState.isNotesMode,
                        canUndo = uiState.canUndo,
                        onUndoClick = { viewModel.undo() },
                        onEraseClick = { viewModel.eraseCell() },
                        onNotesToggle = { viewModel.toggleNotesMode() },
                        onHintClick = { viewModel.requestHint() },
                        onAutoFillNotes = { viewModel.autoFillAllNotes() },
                        onClearNotes = { viewModel.clearAllNotes() }
                    )

                    // Digit Keypad (1 to 9 with counters & touch exploration)
                    SudokuKeypad(
                        digitCounts = uiState.digitCounts,
                        selectedDigit = uiState.selectedNumber,
                        speechFeedbackEnabled = uiState.settings.speechFeedbackEnabled,
                        isHighContrast = uiState.settings.highContrast,
                        onDigitHover = { digit -> viewModel.onDigitHover(digit) },
                        onDigitClick = { digit -> viewModel.enterDigit(digit) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Dialogs
            if (uiState.isWon) {
                VictoryDialog(
                    difficulty = uiState.difficulty,
                    elapsedSeconds = uiState.elapsedSeconds,
                    mistakesCount = uiState.mistakesCount,
                    hintsUsed = uiState.hintsUsed,
                    onDismiss = { viewModel.dismissVictoryDialog() },
                    onNewGame = {
                        viewModel.dismissVictoryDialog()
                        viewModel.startNewGame(uiState.difficulty)
                    }
                )
            }

            if (uiState.isGameOver) {
                GameOverDialog(
                    mistakesCount = uiState.mistakesCount,
                    maxMistakes = uiState.settings.maxMistakes,
                    onRestart = { viewModel.restartCurrentGame() },
                    onNewGame = { viewModel.startNewGame(uiState.difficulty) }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    settings = uiState.settings,
                    onDismiss = { showSettingsDialog = false },
                    onSettingsChanged = { viewModel.updateSettings(it) }
                )
            }

            if (showStatsDialog) {
                StatsDialog(
                    stats = uiState.stats,
                    onDismiss = { showStatsDialog = false }
                )
            }

            if (showAboutDialog) {
                AboutDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }

            if (showPwaDialog) {
                PwaDialog(
                    onDismiss = { showPwaDialog = false },
                    onLaunchPwaViewer = {
                        showPwaDialog = false
                        showPwaWebView = true
                    }
                )
            }

            if (showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { showRestartDialog = false },
                    title = { Text("Puzzel herstarten?") },
                    text = { Text("Wil je deze puzzel helemaal opnieuw beginnen? Je huidige voortgang wordt gewist.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRestartDialog = false
                                viewModel.restartCurrentGame()
                            }
                        ) {
                            Text("Herstarten")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestartDialog = false }) {
                            Text("Annuleren")
                        }
                    }
                )
            }

            pendingDifficultyChange?.let { newDiff ->
                AlertDialog(
                    onDismissRequest = { pendingDifficultyChange = null },
                    title = { Text("Nieuw spel starten?") },
                    text = { Text("Wil je een nieuwe ${newDiff.title} Sudoku starten? Je huidige voortgang wordt vervangen.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingDifficultyChange = null
                                viewModel.startNewGame(newDiff)
                            }
                        ) {
                            Text("Start ${newDiff.title}")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDifficultyChange = null }) {
                            Text("Annuleren")
                        }
                    }
                )
            }
        }
    }
}
