package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SudokuDifficulty
import com.example.model.SudokuSettings

@Composable
fun SudokuHeader(
    difficulty: SudokuDifficulty,
    elapsedSeconds: Long,
    isPaused: Boolean,
    mistakesCount: Int,
    settings: SudokuSettings,
    modifier: Modifier = Modifier,
    onDifficultySelect: (SudokuDifficulty) -> Unit,
    onTogglePause: () -> Unit,
    onRestartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPwaClick: () -> Unit = {}
) {
    var difficultyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("sudoku_header")
    ) {
        // Top App Bar Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty Selector Chip
            Box {
                Surface(
                    onClick = { difficultyMenuExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .testTag("difficulty_selector")
                        .clearAndSetSemantics {
                            contentDescription = "Moeilijkheidsgraad: ${difficulty.title}. Tik om te wijzigen"
                            role = Role.DropdownList
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = difficulty.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = difficultyMenuExpanded,
                    onDismissRequest = { difficultyMenuExpanded = false }
                ) {
                    for (diff in SudokuDifficulty.entries) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = diff.title,
                                        fontWeight = if (diff == difficulty) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = diff.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                difficultyMenuExpanded = false
                                onDifficultySelect(diff)
                            }
                        )
                    }
                }
            }

            // Right side icons: Restart, Stats, Settings, About
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRestartClick,
                    modifier = Modifier.testTag("button_restart")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Puzzel opnieuw starten",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onStatsClick,
                    modifier = Modifier.testTag("button_stats")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Statistieken overzicht openen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("button_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Instellingen openen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPwaClick,
                    modifier = Modifier.testTag("button_pwa")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "PWA Web App versie openen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onAboutClick,
                    modifier = Modifier.testTag("button_about")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Uitleg van de knoppen openen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Status Sub-Bar: Mistakes Counter & Timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mistakes display
            val mistakesText = if (settings.maxMistakesEnabled) {
                "Fouten: $mistakesCount/${settings.maxMistakes}"
            } else {
                "Fouten: $mistakesCount"
            }

            val mistakesA11y = if (settings.maxMistakesEnabled) {
                "Aantal fouten: $mistakesCount van maximaal ${settings.maxMistakes}"
            } else {
                "Aantal fouten: $mistakesCount"
            }

            Text(
                text = mistakesText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (mistakesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = mistakesA11y
                    liveRegion = LiveRegionMode.Polite
                }
            )

            // Timer with Pause/Play toggle
            if (settings.showTimer) {
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val formattedTime = String.format("%02d:%02d", minutes, seconds)

                val timerA11y = "Speeltijd: $minutes minuten en $seconds seconden. " +
                        if (isPaused) "Gepauzeerd. Tik om te hervatten." else "Actief. Tik om te pauzeren."

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClick = onTogglePause,
                            onClickLabel = if (isPaused) "Hervat speeltimer" else "Pauzeer speeltimer",
                            role = Role.Button
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("timer_display")
                        .clearAndSetSemantics {
                            contentDescription = timerA11y
                            role = Role.Button
                        }
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
