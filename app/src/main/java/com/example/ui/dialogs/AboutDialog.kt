package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_about"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Uitleg van de knoppen",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sectie 1: Aanraken & Navigeren
                ButtonCategoryCard(
                    title = "Aanraken & Navigatie",
                    items = listOf(
                        ButtonItemData(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            name = "Vinger over het bord",
                            description = "Glij met je vinger over het bord om direct de cel en het cijfer te horen."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Shuffle,
                            name = "Kruislings",
                            description = "Navigeer diagonaal (schuin) over het speelbord."
                        ),
                        ButtonItemData(
                            name = "Pijlen (Omhoog/Omlaag/Links/Rechts)",
                            description = "Verplaats de selectie één stap in de gewenste richting."
                        )
                    )
                )

                // Sectie 2: Spelbediening
                ButtonCategoryCard(
                    title = "Spelbediening",
                    items = listOf(
                        ButtonItemData(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            name = "Herstel",
                            description = "Maakt je laatste zet of notitiewijziging ongedaan."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Backspace,
                            name = "Wissen",
                            description = "Maakt de geselecteerde cel leeg."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Edit,
                            name = "Notities (Aan / Uit)",
                            description = "Wissel tussen cijfers definitief plaatsen en potloodnotities invoeren."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Lightbulb,
                            name = "Hint",
                            description = "Geeft een logische denkstap of vult de juiste waarde in."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.AutoAwesome,
                            name = "Magie",
                            description = "Menu om automatisch alle mogelijke notities in te vullen of te wissen."
                        )
                    )
                )

                // Sectie 3: Invoer 1 t/m 9
                ButtonCategoryCard(
                    title = "Cijfertoetsen 1 t/m 9",
                    items = listOf(
                        ButtonItemData(
                            name = "Toetsen 1 t/m 9",
                            description = "Plaats het cijfer in de geselecteerde cel. Het kleine getal toont hoeveel er nog nodig zijn."
                        )
                    )
                )

                // Sectie 4: Bovenbalk
                ButtonCategoryCard(
                    title = "Bovenbalk",
                    items = listOf(
                        ButtonItemData(
                            name = "Moeilijkheidsgraad",
                            description = "Kies uit Makkelijk, Gemiddeld, Moeilijk of Expert."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Pause,
                            name = "Pauze / Timer",
                            description = "Pauzeert of hervat het spel en de looptijd."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Refresh,
                            name = "Herstart",
                            description = "Start de huidige puzzel opnieuw met een leeg bord."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.BarChart,
                            name = "Statistieken",
                            description = "Toont gespeelde spellen, beste tijden en winstpercentages."
                        ),
                        ButtonItemData(
                            icon = Icons.Default.Settings,
                            name = "Instellingen",
                            description = "Pas spraak, trillingen, markeringen en thema's aan."
                        )
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_about_close")
            ) {
                Text("Sluiten", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private data class ButtonItemData(
    val name: String,
    val description: String,
    val icon: ImageVector? = null
)

@Composable
private fun ButtonCategoryCard(
    title: String,
    items: List<ButtonItemData>
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            for (item in items) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
