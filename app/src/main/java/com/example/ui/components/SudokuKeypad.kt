package com.example.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SudokuKeypad(
    digitCounts: Map<Int, Int>,
    selectedDigit: Int?,
    speechFeedbackEnabled: Boolean = true,
    isHighContrast: Boolean = false,
    modifier: Modifier = Modifier,
    onDigitHover: (digit: Int) -> Unit = {},
    onDigitClick: (digit: Int) -> Unit
) {
    var lastHoveredDigit by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("sudoku_keypad")
            .pointerInput(speechFeedbackEnabled) {
                if (speechFeedbackEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat()
                        if (w > 0) {
                            val digit = ((down.position.x / (w / 9f)).toInt() + 1).coerceIn(1, 9)
                            lastHoveredDigit = digit
                            onDigitHover(digit)
                        }

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                val currentW = size.width.toFloat()
                                if (currentW > 0) {
                                    val d = ((change.position.x / (currentW / 9f)).toInt() + 1).coerceIn(1, 9)
                                    if (d != lastHoveredDigit) {
                                        lastHoveredDigit = d
                                        onDigitHover(d)
                                    }
                                }
                            } else {
                                lastHoveredDigit = null
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (digit in 1..9) {
                val count = digitCounts[digit] ?: 0
                val isComplete = count >= 9
                val remaining = (9 - count).coerceAtLeast(0)
                val isSelected = selectedDigit == digit

                KeypadDigitButton(
                    digit = digit,
                    remaining = remaining,
                    isComplete = isComplete,
                    isSelected = isSelected,
                    isHighContrast = isHighContrast,
                    modifier = Modifier.weight(1f),
                    onClick = { onDigitClick(digit) }
                )
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: Int,
    remaining: Int,
    isComplete: Boolean,
    isSelected: Boolean,
    isHighContrast: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val containerColor = when {
        isSelected -> if (isHighContrast) {
            if (isDark) Color(0xFF263238) else Color(0xFFFFF176)
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        isComplete -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isHighContrast) 0.6f else 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isSelected -> if (isHighContrast) {
            if (isDark) Color(0xFFFFFF00) else Color(0xFF000000)
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
        isComplete -> if (isHighContrast) {
            if (isDark) Color(0xFF78909C) else Color(0xFF757575)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }
        else -> if (isHighContrast) {
            if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
        } else {
            MaterialTheme.colorScheme.primary
        }
    }

    val borderStroke = when {
        isSelected -> if (isHighContrast) {
            BorderStroke(2.5.dp, if (isDark) Color(0xFFFFFF00) else Color(0xFF000000))
        } else {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        }
        isHighContrast -> BorderStroke(1.5.dp, if (isDark) Color(0xFF90A4AE) else Color(0xFF424242))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val a11yDescription = buildString {
        append("Cijfer $digit. ")
        if (isComplete) {
            append("Compleet. ")
        } else {
            append("$remaining nodig. ")
        }
        if (isSelected) {
            append("Geselecteerd.")
        }
    }.trim()

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .testTag("keypad_digit_$digit")
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
                role = Role.Button
                selected = isSelected
                this.onClick(label = "Cijfer $digit invoeren") {
                    onClick()
                    true
                }
            },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = borderStroke,
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(14.dp)
            ) {
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Nummer $digit compleet",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.height(12.dp)
                    )
                } else {
                    Text(
                        text = "$remaining",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
