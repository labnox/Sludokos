package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SudokuCell
import com.example.ui.theme.SudokuClueDark
import com.example.ui.theme.SudokuClueHighContrastDark
import com.example.ui.theme.SudokuClueHighContrastLight
import com.example.ui.theme.SudokuClueLight
import com.example.ui.theme.SudokuConflict
import com.example.ui.theme.SudokuConflictBg
import com.example.ui.theme.SudokuConflictBgHighContrast
import com.example.ui.theme.SudokuConflictHighContrast
import com.example.ui.theme.SudokuCrosshairDark
import com.example.ui.theme.SudokuCrosshairLight
import com.example.ui.theme.SudokuInputDark
import com.example.ui.theme.SudokuInputHighContrastDark
import com.example.ui.theme.SudokuInputHighContrastLight
import com.example.ui.theme.SudokuInputLight
import com.example.ui.theme.SudokuPencilHighContrastDark
import com.example.ui.theme.SudokuPencilHighContrastLight
import com.example.ui.theme.SudokuSameNumberDark
import com.example.ui.theme.SudokuSameNumberLight
import com.example.ui.theme.SudokuSelectedDark
import com.example.ui.theme.SudokuSelectedHighContrastDark
import com.example.ui.theme.SudokuSelectedHighContrastLight
import com.example.ui.theme.SudokuSelectedLight

@Composable
fun SudokuCellView(
    cell: SudokuCell,
    isSelected: Boolean,
    isCrosshair: Boolean,
    isSameNumber: Boolean,
    hasConflict: Boolean,
    modifier: Modifier = Modifier,
    announceCoordinates: Boolean = false,
    isHighContrast: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Vibrant Yellow/Amber for pencil notes, high contrast on both light and dark backgrounds
    val pencilYellowColor = if (isHighContrast) {
        if (isDark) SudokuPencilHighContrastDark else SudokuPencilHighContrastLight
    } else {
        if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17)
    }
    val hasPencilNotes = cell.value == 0 && cell.notes.isNotEmpty()

    // Determine background color based on status
    val targetBgColor = when {
        hasConflict -> if (isHighContrast) SudokuConflictBgHighContrast else SudokuConflictBg
        isSelected -> if (isHighContrast) {
            if (isDark) SudokuSelectedHighContrastDark else SudokuSelectedHighContrastLight
        } else {
            if (isDark) SudokuSelectedDark else SudokuSelectedLight
        }
        isSameNumber -> if (isHighContrast) {
            if (isDark) Color(0x66FFD600) else Color(0x66FFC107)
        } else {
            if (isDark) SudokuSameNumberDark else SudokuSameNumberLight
        }
        isCrosshair -> if (isHighContrast) {
            if (isDark) Color(0x3390CAF9) else Color(0x221E88E5)
        } else {
            if (isDark) SudokuCrosshairDark else SudokuCrosshairLight
        }
        hasPencilNotes -> if (isDark) Color(0x2AFFF176) else Color(0x26FFF9C4)
        else -> Color.Transparent
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 150),
        label = "cellBg"
    )

    // Build rich, accessible description for TalkBack / Screen Readers
    val accessibilityDescription = buildString {
        if (announceCoordinates) {
            append("Rij ${cell.row + 1}, kolom ${cell.col + 1}. ")
        }
        when {
            cell.isGiven -> append("Vast getal ${cell.value}. ")
            cell.value != 0 -> append("Ingevuld getal ${cell.value}. ")
            cell.notes.isNotEmpty() -> append("Geel potlood getal ${cell.notes.sorted().joinToString(", ")}. ")
            else -> append("Lege cel. ")
        }
        if (hasConflict) append("Conflict met een ander getal ${cell.value}. ")
        if (cell.isHinted) append("Hint ontvangen. ")
        if (isSelected) append("Geselecteerd.")
    }.trim()

    val selectionBorderModifier = if (isSelected && isHighContrast) {
        Modifier.border(
            BorderStroke(3.dp, if (isDark) Color(0xFFFFFF00) else Color(0xFF000000))
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .testTag("cell_${cell.row}_${cell.col}")
            .then(selectionBorderModifier)
            .background(animatedBgColor)
            .clickable(
                onClick = onClick,
                onClickLabel = "Selecteer cel op rij ${cell.row + 1}, kolom ${cell.col + 1}",
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
                selected = isSelected
                role = Role.Button
                collectionItemInfo = CollectionItemInfo(
                    rowIndex = cell.row,
                    rowSpan = 1,
                    columnIndex = cell.col,
                    columnSpan = 1
                )
                this.onClick(label = "Selecteer cel op rij ${cell.row + 1}, kolom ${cell.col + 1}") {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (cell.value != 0) {
            // Main cell number
            val textColor = when {
                hasConflict -> if (isHighContrast) SudokuConflictHighContrast else SudokuConflict
                cell.isGiven -> if (isHighContrast) {
                    if (isDark) SudokuClueHighContrastDark else SudokuClueHighContrastLight
                } else {
                    if (isDark) SudokuClueDark else SudokuClueLight
                }
                cell.isHinted -> if (isHighContrast) {
                    if (isDark) Color(0xFF00FF66) else Color(0xFF007A33)
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
                else -> if (isHighContrast) {
                    if (isDark) SudokuInputHighContrastDark else SudokuInputHighContrastLight
                } else {
                    if (isDark) SudokuInputDark else SudokuInputLight
                }
            }

            Text(
                text = cell.value.toString(),
                fontSize = if (isHighContrast) 26.sp else 24.sp,
                fontWeight = if (isHighContrast) FontWeight.Black else if (cell.isGiven) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        } else if (cell.notes.isNotEmpty()) {
            // Pencil Notes: Yellow and large
            if (cell.notes.size == 1) {
                // Exactly as big as the fixed numbers!
                Text(
                    text = cell.notes.first().toString(),
                    fontSize = if (isHighContrast) 26.sp else 24.sp,
                    fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Bold,
                    color = pencilYellowColor,
                    textAlign = TextAlign.Center
                )
            } else if (cell.notes.size == 2) {
                Text(
                    text = cell.notes.sorted().joinToString(" "),
                    fontSize = if (isHighContrast) 20.sp else 19.sp,
                    fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Bold,
                    color = pencilYellowColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = cell.notes.sorted().chunked(3).joinToString("\n") { it.joinToString(" ") },
                    fontSize = if (cell.notes.size <= 4) 16.sp else 13.sp,
                    fontWeight = if (isHighContrast) FontWeight.Black else FontWeight.Bold,
                    color = pencilYellowColor,
                    textAlign = TextAlign.Center,
                    lineHeight = if (cell.notes.size <= 4) 18.sp else 14.sp
                )
            }
        }
    }
}
