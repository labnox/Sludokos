package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance, zero-latency procedural sound effect engine designed specifically
 * for accessibility (TalkBack) and tactile audio feedback in Sudoku.
 *
 * Generates custom 16-bit PCM waveform cues:
 * - Number Selection: Musical chime mapped to digits 1..9 (harmonic pentatonic/major scale).
 * - Error Alert: Dissonant, low warning buzzer cue.
 * - Puzzle Success / Victory: Triumphant 4-note ascending major fanfare.
 * - Cell Selection: Crisp, subtle acoustic tick.
 * - Erase: Soft downward sweep.
 */
class SoundEffectManager {

    var isEnabled: Boolean = true

    enum class SoundType {
        DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4, DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
        GENERIC_DIGIT_SELECT,
        ERROR_ALERT,
        PUZZLE_SUCCESS,
        CELL_SELECT,
        ERASE
    }

    private val sampleRate = 22050
    private val tracks = ConcurrentHashMap<SoundType, AudioTrack>()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        executor.execute {
            preloadSounds()
        }
    }

    private fun preloadSounds() {
        try {
            // Harmonic frequencies for digits 1..9:
            val digitFreqs = listOf(
                523.25, // C5 (1)
                587.33, // D5 (2)
                659.25, // E5 (3)
                698.46, // F5 (4)
                783.99, // G5 (5)
                880.00, // A5 (6)
                987.77, // B5 (7)
                1046.50, // C6 (8)
                1174.66  // D6 (9)
            )

            val soundTypes = listOf(
                SoundType.DIGIT_1, SoundType.DIGIT_2, SoundType.DIGIT_3,
                SoundType.DIGIT_4, SoundType.DIGIT_5, SoundType.DIGIT_6,
                SoundType.DIGIT_7, SoundType.DIGIT_8, SoundType.DIGIT_9
            )

            digitFreqs.forEachIndexed { index, freq ->
                createStaticTrack(soundTypes[index], generateChime(freq, 0.08))
            }

            createStaticTrack(SoundType.GENERIC_DIGIT_SELECT, generateChime(659.25, 0.08))
            createStaticTrack(SoundType.ERROR_ALERT, generateErrorBuzz())
            createStaticTrack(SoundType.PUZZLE_SUCCESS, generateVictoryFanfare())
            createStaticTrack(SoundType.CELL_SELECT, generateTick())
            createStaticTrack(SoundType.ERASE, generateSweep(440.0, 220.0, 0.09))
        } catch (_: Throwable) {
            // Graceful fallback if audio is not supported in environment
        }
    }

    private fun createStaticTrack(type: SoundType, pcmData: ShortArray) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val byteBufferSize = pcmData.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(byteBufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            tracks[type] = track
        } catch (_: Throwable) {}
    }

    /**
     * Plays a crisp, melodic audio cue for selecting or entering a digit (1..9).
     */
    fun playNumberSelect(digit: Int) {
        if (!isEnabled) return
        val type = when (digit) {
            1 -> SoundType.DIGIT_1
            2 -> SoundType.DIGIT_2
            3 -> SoundType.DIGIT_3
            4 -> SoundType.DIGIT_4
            5 -> SoundType.DIGIT_5
            6 -> SoundType.DIGIT_6
            7 -> SoundType.DIGIT_7
            8 -> SoundType.DIGIT_8
            9 -> SoundType.DIGIT_9
            else -> SoundType.GENERIC_DIGIT_SELECT
        }
        playTrack(type)
    }

    /**
     * Plays an unmistakable, dissonant warning sound cue for errors and conflicts.
     */
    fun playErrorAlert() {
        if (!isEnabled) return
        playTrack(SoundType.ERROR_ALERT)
    }

    /**
     * Plays a triumphant, multi-note celebratory arpeggio for puzzle completion.
     */
    fun playPuzzleSuccess() {
        if (!isEnabled) return
        playTrack(SoundType.PUZZLE_SUCCESS)
    }

    /**
     * Plays a subtle, tactile click when navigating or tapping cells.
     */
    fun playCellSelect() {
        if (!isEnabled) return
        playTrack(SoundType.CELL_SELECT)
    }

    /**
     * Plays a gentle clearing sweep sound when erasing a cell.
     */
    fun playErase() {
        if (!isEnabled) return
        playTrack(SoundType.ERASE)
    }

    private fun playTrack(type: SoundType) {
        executor.execute {
            try {
                val track = tracks[type] ?: return@execute
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                }
            } catch (_: Throwable) {}
        }
    }

    fun release() {
        executor.execute {
            tracks.values.forEach { track ->
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
            tracks.clear()
        }
        executor.shutdown()
    }

    // --- Procedural PCM Waveform Generators ---

    private fun generateChime(freq: Double, durationSec: Double): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Fundamental + octave overtone + quick exponential decay envelope
            val envelope = exp(-t * 22.0)
            val wave = 0.75 * sin(2.0 * PI * freq * t) + 0.25 * sin(2.0 * PI * (freq * 2.0) * t)
            samples[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateErrorBuzz(): ShortArray {
        // Dissonant dual tone (220 Hz and 233 Hz) with double pulse (beep-beep)
        val durationSec = 0.22
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val halfSamples = numSamples / 2
        val silenceSamples = (sampleRate * 0.03).toInt()

        for (i in 0 until numSamples) {
            val isFirstBeep = i < (halfSamples - silenceSamples)
            val isSecondBeep = i >= halfSamples
            if (isFirstBeep || isSecondBeep) {
                val t = i.toDouble() / sampleRate
                // Harsh minor 2nd clash for unmistakable acoustic warning
                val wave = 0.5 * sin(2.0 * PI * 220.0 * t) + 0.5 * sin(2.0 * PI * 233.08 * t)
                val decay = 1.0 - ((i % halfSamples).toDouble() / halfSamples) * 0.5
                samples[i] = (wave * decay * Short.MAX_VALUE * 0.65).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else {
                samples[i] = 0
            }
        }
        return samples
    }

    private fun generateVictoryFanfare(): ShortArray {
        // Triumphant 4-note ascending major arpeggio: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
        val noteDurations = listOf(0.09, 0.09, 0.09, 0.28)
        val noteFreqs = listOf(523.25, 659.25, 783.99, 1046.50)
        val totalSamples = noteDurations.sumOf { (it * sampleRate).toInt() }
        val samples = ShortArray(totalSamples)

        var sampleOffset = 0
        for (n in noteFreqs.indices) {
            val freq = noteFreqs[n]
            val duration = noteDurations[n]
            val noteSamples = (duration * sampleRate).toInt()
            val isFinal = (n == noteFreqs.size - 1)

            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = if (isFinal) {
                    exp(-t * 5.0) // Long resonant ring-out on the tonic
                } else {
                    exp(-t * 12.0)
                }
                // Rich bell-like harmonics: fundamental + 2nd + 3rd
                val wave = 0.6 * sin(2.0 * PI * freq * t) +
                           0.3 * sin(2.0 * PI * (freq * 2.0) * t) +
                           0.1 * sin(2.0 * PI * (freq * 3.0) * t)
                val totalIndex = sampleOffset + i
                if (totalIndex < totalSamples) {
                    samples[totalIndex] = (wave * envelope * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            sampleOffset += noteSamples
        }
        return samples
    }

    private fun generateTick(): ShortArray {
        val numSamples = (sampleRate * 0.035).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 80.0)
            val wave = sin(2.0 * PI * 880.0 * t)
            samples[i] = (wave * envelope * Short.MAX_VALUE * 0.45).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateSweep(startFreq: Double, endFreq: Double, durationSec: Double): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * frac
            val t = i.toDouble() / sampleRate
            val envelope = 1.0 - frac
            val wave = sin(2.0 * PI * freq * t)
            samples[i] = (wave * envelope * Short.MAX_VALUE * 0.5).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }
}
