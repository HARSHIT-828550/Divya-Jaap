package com.example.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesizes a serene resonant bronze temple bell sound offline using AudioTrack,
 * eliminating external sound file dependencies while giving authentic audio feedback
 * when a 108-bead (or custom target) Mala completes.
 */
class TempleBellPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bellSamples: ShortArray by lazy { generateTempleBellSamples() }

    fun playBell() {
        scope.launch {
            runCatching {
                val sampleRate = SAMPLE_RATE
                val samples = bellSamples
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(samples.size * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuffer)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                kotlinx.coroutines.delay(DURATION_MS)
                track.stop()
                track.release()
            }
        }
    }

    private fun generateTempleBellSamples(): ShortArray {
        val totalSamples = ((SAMPLE_RATE * DURATION_MS) / 1000L).toInt()
        val buffer = ShortArray(totalSamples)
        // Bronze temple bell harmonic partials with natural beat shimmer
        val partials = listOf(
            432.0 to 0.50,
            435.5 to 0.25,
            864.0 to 0.28,
            1298.0 to 0.14,
            1732.0 to 0.08
        )
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.012).coerceAtMost(1.0)
            val envelope = attack * exp(-t * 2.1)
            var sum = 0.0
            for ((freq, amp) in partials) {
                sum += amp * sin(2.0 * PI * freq * t)
            }
            val value = (sum * envelope * Short.MAX_VALUE * 0.55)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = value.toShort()
        }
        return buffer
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val DURATION_MS = 1800L
    }
}
