package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SudokuControls(
    isNotesMode: Boolean,
    canUndo: Boolean,
    modifier: Modifier = Modifier,
    onUndoClick: () -> Unit,
    onEraseClick: () -> Unit,
    onNotesToggle: () -> Unit,
    onHintClick: () -> Unit,
    onAutoFillNotes: () -> Unit,
    onClearNotes: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("sudoku_controls"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo Action
        SudokuActionButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = "Herstel",
            actionDescription = "Laatste zet ongedaan maken",
            enabled = canUndo,
            testTag = "action_undo",
            onClick = onUndoClick
        )

        // Erase Action
        SudokuActionButton(
            icon = Icons.Default.Backspace,
            label = "Wissen",
            actionDescription = "Geselecteerde cel leegmaken",
            testTag = "action_erase",
            onClick = onEraseClick
        )

        // Notes Mode Toggle (With active pill)
        SudokuNotesButton(
            isActive = isNotesMode,
            testTag = "action_notes",
            onClick = onNotesToggle
        )

        // Hint Action
        SudokuActionButton(
            icon = Icons.Default.Lightbulb,
            label = "Hint",
            actionDescription = "Vraag een logische hint aan",
            testTag = "action_hint",
            onClick = onHintClick
        )

        // Magic Auto-fill / Clear Notes Menu
        Box {
            SudokuActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Magie",
                actionDescription = "Menu voor automatisch notities invullen of wissen openen",
                testTag = "action_magic_notes",
                onClick = { showMoreMenu = true }
            )

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Vul alle mogelijke notities in") },
                    onClick = {
                        showMoreMenu = false
                        onAutoFillNotes()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Wis alle notities van het bord") },
                    onClick = {
                        showMoreMenu = false
                        onClearNotes()
                    }
                )
            }
        }
    }
}

@Composable
private fun SudokuActionButton(
    icon: ImageVector,
    label: String,
    actionDescription: String,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick,
                onClickLabel = actionDescription,
                role = Role.Button
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (enabled) "$label: $actionDescription" else "$label: niet beschikbaar"
                role = Role.Button
                if (enabled) {
                    this.onClick(label = actionDescription) {
                        onClick()
                        true
                    }
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.6f else 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SudokuNotesButton(
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        label = "notesBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "notesIcon"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = onClick,
                onClickLabel = if (isActive) "Schakel notitiemodus uit" else "Schakel notitiemodus in",
                role = Role.Switch
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "Notities modus"
                stateDescription = if (isActive) "Ingeschakeld" else "Uitgeschakeld"
                role = Role.Switch
                toggleableState = if (isActive) ToggleableState.On else ToggleableState.Off
                this.onClick(label = if (isActive) "Schakel notitiemodus uit" else "Schakel notitiemodus in") {
                    onClick()
                    true
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = bgColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            // Mini Badge for Active State
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(10.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }

        Text(
            text = if (isActive) "Notities: AAN" else "Notities: UIT",
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
