package com.example.ui.dialogs

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.SudokuSettings

@Composable
fun SettingsDialog(
    settings: SudokuSettings,
    onDismiss: () -> Unit,
    onSettingsChanged: (SudokuSettings) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_settings"),
        title = {
            Text(
                text = "Instellingen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Gameplay
                Text(
                    text = "Spelopties",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingSwitchItem(
                    title = "Markeer duplicaten",
                    subtitle = "Toon conflicten in rood",
                    checked = settings.highlightDuplicates,
                    onCheckedChange = { onSettingsChanged(settings.copy(highlightDuplicates = it)) }
                )

                SettingSwitchItem(
                    title = "Markeer rij, kolom en blok",
                    subtitle = "Kruisdraad highlight rondom geselecteerde cel",
                    checked = settings.highlightCrosshair,
                    onCheckedChange = { onSettingsChanged(settings.copy(highlightCrosshair = it)) }
                )

                SettingSwitchItem(
                    title = "Markeer gelijke cijfers",
                    subtitle = "Licht alle identieke getallen op",
                    checked = settings.highlightSameNumber,
                    onCheckedChange = { onSettingsChanged(settings.copy(highlightSameNumber = it)) }
                )

                SettingSwitchItem(
                    title = "Notities automatisch wissen",
                    subtitle = "Verwijder notities in rij/kolom/blok bij plaatsing",
                    checked = settings.autoRemoveNotes,
                    onCheckedChange = { onSettingsChanged(settings.copy(autoRemoveNotes = it)) }
                )

                SettingSwitchItem(
                    title = "Trillen bij aanraking",
                    subtitle = "Haptische feedback bij invoer",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(vibrationEnabled = it)) }
                )

                SettingSwitchItem(
                    title = "Foutenlimiet (3 fouten)",
                    subtitle = "Spel eindigt bij 3 onjuiste cijfers",
                    checked = settings.maxMistakesEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(maxMistakesEnabled = it)) }
                )

                SettingSwitchItem(
                    title = "Timer tonen",
                    subtitle = "Toon de verstreken speeltijd",
                    checked = settings.showTimer,
                    onCheckedChange = { onSettingsChanged(settings.copy(showTimer = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Section: Accessibility & Speech
                Text(
                    text = "Spraak & Toegankelijkheid",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingSwitchItem(
                    title = "Hoog contrast (High Contrast)",
                    subtitle = "Extra duidelijke rasterlijnen en hoog-contrast kleuren voor slechtzienden",
                    checked = settings.highContrast,
                    onCheckedChange = { onSettingsChanged(settings.copy(highContrast = it)) }
                )

                SettingSwitchItem(
                    title = "Navigatietoetsen tonen",
                    subtitle = "Toon pijlen en kruislings onder het bord (standaard verborgen)",
                    checked = settings.showNavButtons,
                    onCheckedChange = { onSettingsChanged(settings.copy(showNavButtons = it)) }
                )

                SettingSwitchItem(
                    title = "Rij en kolom uitspreken",
                    subtitle = "Spreek posities uit bij aanraking (standaard alleen het cijfer)",
                    checked = settings.speechAnnounceCoordinates,
                    onCheckedChange = { onSettingsChanged(settings.copy(speechAnnounceCoordinates = it)) }
                )

                SettingSwitchItem(
                    title = "Spraakfeedback bij aanraken",
                    subtitle = "Spreek cijfers en knoppen direct uit bij aanraking",
                    checked = settings.speechFeedbackEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(speechFeedbackEnabled = it)) }
                )

                SettingSwitchItem(
                    title = "Geluidseffecten (Audio-cues)",
                    subtitle = "Geluidssignalen bij cijferselectie, foutwaarschuwingen en puzzelwinst",
                    checked = settings.soundEffectsEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(soundEffectsEnabled = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Section: Theme
                Text(
                    text = "Thema",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ThemeRadioOption(
                    title = "Systeemstandaard",
                    selected = settings.themeMode == AppThemeMode.SYSTEM,
                    onClick = { onSettingsChanged(settings.copy(themeMode = AppThemeMode.SYSTEM)) }
                )

                ThemeRadioOption(
                    title = "Licht",
                    selected = settings.themeMode == AppThemeMode.LIGHT,
                    onClick = { onSettingsChanged(settings.copy(themeMode = AppThemeMode.LIGHT)) }
                )

                ThemeRadioOption(
                    title = "Donker",
                    selected = settings.themeMode == AppThemeMode.DARK,
                    onClick = { onSettingsChanged(settings.copy(themeMode = AppThemeMode.DARK)) }
                )

                ThemeRadioOption(
                    title = "Sepia / Papier",
                    selected = settings.themeMode == AppThemeMode.SEPIA,
                    onClick = { onSettingsChanged(settings.copy(themeMode = AppThemeMode.SEPIA)) }
                )

                ThemeRadioOption(
                    title = "Midnight (OLED Zwart)",
                    selected = settings.themeMode == AppThemeMode.MIDNIGHT,
                    onClick = { onSettingsChanged(settings.copy(themeMode = AppThemeMode.MIDNIGHT)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_settings_close")
            ) {
                Text("Sluiten")
            }
        }
    )
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null // Delegated to row toggleable for unified accessibility node
        )
    }
}

@Composable
private fun ThemeRadioOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // Delegated to row selectable
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
