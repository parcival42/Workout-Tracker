package io.github.parcival42.workouttracker.ui.circuit

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Sounds and vibration for the circuit. The tones are soft sine beeps generated in code (no
 * sound files): three ticks before every phase change, then a long tone when a station starts
 * or a higher short one when the rest starts, and a small rising chime at the end. They play
 * as media, so they follow the media volume, reach headphones and are not muted by silent mode.
 */
class CircuitSounds(context: Context) {
    private val tick = track(note(TICK_HZ, 90))
    private val stationStart = track(note(TICK_HZ, 650))
    private val restStart = track(note(REST_HZ, 160))
    private val finished = track(note(TICK_HZ, 180) + note(CHIME_MIDDLE_HZ, 180) + note(REST_HZ, 450))

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }?.takeIf { it.hasVibrator() }

    fun play(event: CircuitEvent) {
        when (event) {
            CircuitEvent.TICK -> tick.replay()
            CircuitEvent.COUNTDOWN_START -> Unit
            CircuitEvent.STATION_START -> {
                stationStart.replay()
                vibrate(longArrayOf(0, 400))
            }
            CircuitEvent.REST_START -> {
                restStart.replay()
                vibrate(longArrayOf(0, 150, 100, 150))
            }
            CircuitEvent.FINISHED -> {
                finished.replay()
                vibrate(longArrayOf(0, 300, 150, 300, 150, 300))
            }
        }
    }

    fun release() {
        listOf(tick, stationStart, restStart, finished).forEach { it?.release() }
    }

    private fun vibrate(pattern: LongArray) {
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val AMPLITUDE = 0.5
        const val ATTACK_MILLIS = 5
        // An A major triad: ticks and the station tone on A5, the rest tone an octave higher.
        const val TICK_HZ = 880.0
        const val CHIME_MIDDLE_HZ = 1_108.73
        const val REST_HZ = 1_760.0

        /** A sine tone with a short fade-in and an exponential fade-out, as 16-bit PCM. */
        fun note(frequency: Double, durationMillis: Int): ShortArray {
            val samples = SAMPLE_RATE * durationMillis / 1_000
            val attack = SAMPLE_RATE * ATTACK_MILLIS / 1_000
            return ShortArray(samples) { i ->
                val fadeIn = min(1.0, i.toDouble() / attack)
                val fadeOut = exp(-4.0 * i / samples)
                val value = sin(2 * PI * frequency * i / SAMPLE_RATE) * AMPLITUDE * fadeIn * fadeOut
                (value * Short.MAX_VALUE).toInt().toShort()
            }
        }

        // Null if the audio system refuses the track; the circuit then just stays silent.
        fun track(pcm: ShortArray): AudioTrack? = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
                .build()
                .apply { write(pcm, 0, pcm.size) }
        }.getOrNull()

        // A static track plays its buffer from the start again after stop + reload.
        fun AudioTrack?.replay() {
            this ?: return
            runCatching {
                stop()
                reloadStaticData()
                play()
            }
        }
    }
}
