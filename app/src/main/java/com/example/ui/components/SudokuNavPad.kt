package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SudokuCell

@Composable
fun SudokuPositionBar(
    selectedCell: SudokuCell?,
    onRepeatAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row = (selectedCell?.row ?: 0) + 1
    val col = (selectedCell?.col ?: 0) + 1
    val block = if (selectedCell != null) (selectedCell.row / 3) * 3 + (selectedCell.col / 3) + 1 else 1

    val cellText = when {
        selectedCell == null -> "Geen cel geselecteerd"
        selectedCell.isGiven -> "Getal ${selectedCell.value} (vast)"
        selectedCell.value != 0 -> "Getal ${selectedCell.value}"
        selectedCell.notes.isNotEmpty() -> "Geel potlood: ${selectedCell.notes.sorted().joinToString(", ")}"
        else -> "Leeg"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(
                onClick = onRepeatAnnouncement,
                onClickLabel = "Spreek huidige positie en inhoud uit",
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "Positie: Rij $row, Kolom $col, Blok $block. Inhoud: $cellText. Dubbeltik om uit te spreken."
                role = Role.Button
                this.onClick(label = "Spreek huidige positie en inhoud uit") {
                    onRepeatAnnouncement()
                    true
                }
            }
            .testTag("sudoku_position_bar"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Rij $row, Kol $col (Blok $block):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = cellText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Herhaal spraak",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SudokuDirectionalBar(
    onNavigate: (dRow: Int, dCol: Int, directionName: String) -> Unit,
    onNavigateKruislings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("sudoku_directional_bar")
            .clearAndSetSemantics {
                contentDescription = "Navigatieknoppen: Links, Omhoog, Kruislings, Omlaag, Rechts"
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DirectionButton(
            icon = Icons.Default.ArrowBack,
            label = "Links",
            accessibilityLabel = "Navigeer naar links",
            testTag = "nav_left",
            onClick = { onNavigate(0, -1, "Links") }
        )

        DirectionButton(
            icon = Icons.Default.ArrowUpward,
            label = "Omhoog",
            accessibilityLabel = "Navigeer verticaal omhoog",
            testTag = "nav_up",
            onClick = { onNavigate(-1, 0, "Omhoog") }
        )

        DirectionButton(
            icon = Icons.Default.Shuffle,
            label = "Kruislings",
            accessibilityLabel = "Navigeer kruislings diagonaal",
            testTag = "nav_cross",
            isAccent = true,
            onClick = onNavigateKruislings
        )

        DirectionButton(
            icon = Icons.Default.ArrowDownward,
            label = "Omlaag",
            accessibilityLabel = "Navigeer verticaal omlaag",
            testTag = "nav_down",
            onClick = { onNavigate(1, 0, "Omlaag") }
        )

        DirectionButton(
            icon = Icons.Default.ArrowForward,
            label = "Rechts",
            accessibilityLabel = "Navigeer naar rechts",
            testTag = "nav_right",
            onClick = { onNavigate(0, 1, "Rechts") }
        )
    }
}

@Composable
private fun DirectionButton(
    icon: ImageVector,
    label: String,
    accessibilityLabel: String,
    testTag: String,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isAccent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (isAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = onClick,
                onClickLabel = accessibilityLabel,
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
                role = Role.Button
                this.onClick(label = accessibilityLabel) {
                    onClick()
                    true
                }
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = bgColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
