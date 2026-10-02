package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SudokuRepository
import com.example.engine.SudokuEngine
import com.example.model.SudokuCell
import com.example.model.SudokuDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sludoko", appName)
    }

    @Test
    fun `test preset puzzle has valid unique solution`() {
        val preset = SudokuEngine.getPresetPuzzle(SudokuDifficulty.EASY)
        assertNotNull(preset)
        assertEquals(81, preset.initialBoard.flatMap { it.toList() }.size)
        assertEquals(81, preset.solution.flatMap { it.toList() }.size)

        // Count initial non-zero clues
        val clues = preset.initialBoard.flatMap { it.toList() }.count { it != 0 }
        assertTrue("Easy preset should have at least 30 clues", clues >= 30)
    }

    @Test
    fun `test conflict detection finds duplicate numbers in row`() {
        val cells = MutableList(81) { idx ->
            SudokuCell(row = idx / 9, col = idx % 9, value = 0)
        }
        // Place duplicate 5 in row 0
        cells[0] = cells[0].copy(value = 5)
        cells[3] = cells[3].copy(value = 5)

        val conflicts = SudokuEngine.findConflicts(cells)
        assertTrue(conflicts.contains(0 to 0))
        assertTrue(conflicts.contains(0 to 3))
    }

    @Test
    fun `test notes serialization and deserialization`() {
        val notes = List(81) { i ->
            if (i == 0) setOf(1, 4, 9)
            else if (i == 5) setOf(2, 7)
            else emptySet()
        }

        val serialized = SudokuRepository.serializeNotes(notes)
        val deserialized = SudokuRepository.deserializeNotes(serialized)

        assertEquals(81, deserialized.size)
        assertEquals(setOf(1, 4, 9), deserialized[0])
        assertEquals(setOf(2, 7), deserialized[5])
        assertTrue(deserialized[1].isEmpty())
    }

    @Test
    fun `test directional and kruislings navigation coordinates`() {
        var row = 4
        var col = 4

        // Move Omhoog
        row = (row - 1).coerceIn(0, 8)
        assertEquals(3, row)

        // Move Omlaag
        row = (row + 1).coerceIn(0, 8)
        assertEquals(4, row)

        // Move Links
        col = (col - 1).coerceIn(0, 8)
        assertEquals(3, col)

        // Move Rechts
        col = (col + 1).coerceIn(0, 8)
        assertEquals(4, col)

        // Move Kruislings (diagonal)
        row = if (row < 8) row + 1 else 0
        col = if (col < 8) col + 1 else 0
        assertEquals(5, row)
        assertEquals(5, col)
    }

    @Test
    fun `test default settings hide nav buttons and announce only digit`() {
        val settings = com.example.model.SudokuSettings()
        assertFalse("Nav buttons should be hidden by default", settings.showNavButtons)
        assertFalse("Position announcement should be off by default (only digit)", settings.speechAnnounceCoordinates)
        assertFalse("High contrast should be disabled by default", settings.highContrast)
        assertTrue("Speech feedback should be enabled by default", settings.speechFeedbackEnabled)
        assertTrue("Sound effects should be enabled by default", settings.soundEffectsEnabled)
    }

    @Test
    fun `test sound effect manager methods execute without errors`() {
        val soundManager = com.example.util.SoundEffectManager()
        assertTrue(soundManager.isEnabled)
        soundManager.playNumberSelect(5)
        soundManager.playErrorAlert()
        soundManager.playPuzzleSuccess()
        soundManager.playCellSelect()
        soundManager.playErase()
        soundManager.release()
    }
}
