package com.example.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.SudokuCell
import com.example.model.SudokuSettings
import com.example.ui.theme.SudokuGridMajor
import com.example.ui.theme.SudokuGridMajorHighContrastDark
import com.example.ui.theme.SudokuGridMajorHighContrastLight
import com.example.ui.theme.SudokuGridMinor
import com.example.ui.theme.SudokuGridMinorDark
import com.example.ui.theme.SudokuGridMinorHighContrastDark
import com.example.ui.theme.SudokuGridMinorHighContrastLight

@Composable
fun SudokuGrid(
    board: List<SudokuCell>,
    selectedRow: Int?,
    selectedCol: Int?,
    conflicts: Set<Pair<Int, Int>>,
    settings: SudokuSettings,
    modifier: Modifier = Modifier,
    boardSize: Dp? = null,
    onCellClick: (row: Int, col: Int) -> Unit,
    onCellHover: (row: Int, col: Int) -> Unit = { _, _ -> },
    onCellTouch: (row: Int, col: Int, isInitialDown: Boolean) -> Unit = { _, _, _ -> }
) {
    val isDark = isSystemInDarkTheme()
    val isHighContrast = settings.highContrast

    val minorLineColor = when {
        isHighContrast -> if (isDark) SudokuGridMinorHighContrastDark else SudokuGridMinorHighContrastLight
        isDark -> SudokuGridMinorDark
        else -> SudokuGridMinor
    }
    val majorLineColor = when {
        isHighContrast -> if (isDark) SudokuGridMajorHighContrastDark else SudokuGridMajorHighContrastLight
        else -> SudokuGridMajor
    }
    val borderStrokeWidth = if (isHighContrast) 3.5.dp else 2.dp

    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val isTouchExploration = accessibilityManager?.isTouchExplorationEnabled == true

    val selectedCell = if (selectedRow != null && selectedCol != null) {
        board.getOrNull(selectedRow * 9 + selectedCol)
    } else null
    val selectedValue = selectedCell?.value?.takeIf { it != 0 }

    var lastHoveredCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("sudoku_grid_container"),
        contentAlignment = Alignment.Center
    ) {
        val boardModifier = if (boardSize != null) {
            Modifier.size(boardSize)
        } else {
            Modifier
                .widthIn(max = 480.dp)
                .aspectRatio(1f)
        }

        Card(
            modifier = boardModifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(borderStrokeWidth, majorLineColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(board, settings.speechFeedbackEnabled, isTouchExploration) {
                        if (!isTouchExploration) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w > 0 && h > 0) {
                                    val c = (down.position.x / (w / 9f)).toInt().coerceIn(0, 8)
                                    val r = (down.position.y / (h / 9f)).toInt().coerceIn(0, 8)
                                    lastHoveredCell = r to c
                                    onCellHover(r, c)
                                    onCellTouch(r, c, true)
                                    onCellClick(r, c)
                                }

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val currentW = size.width.toFloat()
                                        val currentH = size.height.toFloat()
                                        if (currentW > 0 && currentH > 0) {
                                            val c = (change.position.x / (currentW / 9f)).toInt().coerceIn(0, 8)
                                            val r = (change.position.y / (currentH / 9f)).toInt().coerceIn(0, 8)
                                            if (lastHoveredCell != (r to c)) {
                                                lastHoveredCell = r to c
                                                onCellHover(r, c)
                                                onCellTouch(r, c, false)
                                            }
                                        }
                                        if ((change.position - down.position).getDistance() > 6f) {
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
            ) {
                // 9x9 Cells Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    for (r in 0 until 9) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            for (c in 0 until 9) {
                                val cell = board.getOrNull(r * 9 + c)
                                    ?: SudokuCell(row = r, col = c)

                                val isSelected = r == selectedRow && c == selectedCol
                                val isCrosshair = settings.highlightCrosshair &&
                                        selectedRow != null && selectedCol != null &&
                                        (r == selectedRow || c == selectedCol ||
                                                (r / 3 == selectedRow / 3 && c / 3 == selectedCol / 3))

                                val isSameNumber = settings.highlightSameNumber &&
                                        selectedValue != null && cell.value == selectedValue

                                val hasConflict = settings.highlightDuplicates &&
                                        conflicts.contains(r to c)

                                SudokuCellView(
                                    cell = cell,
                                    isSelected = isSelected,
                                    isCrosshair = isCrosshair,
                                    isSameNumber = isSameNumber,
                                    hasConflict = hasConflict,
                                    announceCoordinates = settings.speechAnnounceCoordinates,
                                    isHighContrast = isHighContrast,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    onClick = { onCellClick(r, c) }
                                )
                            }
                        }
                    }
                }

                // Grid Lines Canvas Overlay (Decorative only - hide from screen reader)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearAndSetSemantics { }
                ) {
                    val stepW = size.width / 9f
                    val stepH = size.height / 9f

                    // Vertical Lines
                    for (i in 1 until 9) {
                        val isThick = i % 3 == 0
                        val strokeW = if (isThick) {
                            if (isHighContrast) 3.5.dp.toPx() else 2.5.dp.toPx()
                        } else {
                            if (isHighContrast) 1.5.dp.toPx() else 0.8.dp.toPx()
                        }
                        val color = if (isThick) majorLineColor else minorLineColor
                        val x = i * stepW
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeW
                        )
                    }

                    // Horizontal Lines
                    for (i in 1 until 9) {
                        val isThick = i % 3 == 0
                        val strokeW = if (isThick) {
                            if (isHighContrast) 3.5.dp.toPx() else 2.5.dp.toPx()
                        } else {
                            if (isHighContrast) 1.5.dp.toPx() else 0.8.dp.toPx()
                        }
                        val color = if (isThick) majorLineColor else minorLineColor
                        val y = i * stepH
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW
                        )
                    }
                }
            }
        }
    }
}

