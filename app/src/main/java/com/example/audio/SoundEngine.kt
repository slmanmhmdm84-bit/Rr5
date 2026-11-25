package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.EnumMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
    PUNCH_SWING,
    HIT_LIGHT,
    HIT_HEAVY,
    BLOCK,
    SPECIAL,
    JUMP,
    DASH,
    ROUND_START,
    KO,
    VICTORY
}

class SoundEngine {
    private val sampleRate = 22050
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ArenaSoundThread").apply { priority = Thread.NORM_PRIORITY }
    }
    @Volatile
    var isMuted: Boolean = false

    @Volatile
    private var released = false

    private val cache: Map<SoundEffect, ShortArray> by lazy {
        EnumMap<SoundEffect, ShortArray>(SoundEffect::class.java).apply {
            put(SoundEffect.PUNCH_SWING, synthNoiseSweep(0.08f, 520f, 180f, 0.32f))
            put(SoundEffect.HIT_LIGHT, synthImpact(0.10f, 240f, 70f, 0.58f, 0.38f))
            put(SoundEffect.HIT_HEAVY, synthImpact(0.18f, 160f, 42f, 0.82f, 0.52f))
            put(SoundEffect.BLOCK, synthMetallic(0.12f, 880f, 1320f, 0.45f))
            put(SoundEffect.SPECIAL, synthLaserCharge(0.28f, 260f, 1180f, 0.60f))
            put(SoundEffect.JUMP, synthToneSweep(0.10f, 240f, 540f, 0.28f))
            put(SoundEffect.DASH, synthNoiseSweep(0.10f, 900f, 300f, 0.26f))
            put(SoundEffect.ROUND_START, synthChord(0.26f, floatArrayOf(440f, 554.37f, 659.25f), 0.42f))
            put(SoundEffect.KO, synthImpact(0.40f, 130f, 35f, 0.88f, 0.55f))
            put(SoundEffect.VICTORY, synthArpeggio(0.44f, floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.42f))
        }
    }

    private val trackPool = EnumMap<SoundEffect, AudioTrack>(SoundEffect::class.java)
    private val audioAttrs by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
    private val audioFormat by lazy {
        AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
    }

    fun warmUp() {
        executor.execute {
            if (released) return@execute
            cache.keys.forEach { effect ->
                getOrCreateTrack(effect)
            }
        }
    }

    fun play(effect: SoundEffect) {
        if (isMuted || released) return
        executor.execute {
            if (isMuted || released) return@execute
            try {
                val track = getOrCreateTrack(effect) ?: return@execute
                if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
            } catch (_: Throwable) {
                // Audio playback unavailable on headless/test devices
            }
        }
    }

    fun release() {
        released = true
        executor.execute {
            trackPool.values.forEach { track ->
                try {
                    if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                        track.stop()
                    }
                    track.release()
                } catch (_: Throwable) {
                }
            }
            trackPool.clear()
        }
        executor.shutdown()
    }

    private fun getOrCreateTrack(effect: SoundEffect): AudioTrack? {
        trackPool[effect]?.let { return it }
        return try {
            val samples = cache[effect] ?: return null
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttrs)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            trackPool[effect] = track
            track
        } catch (_: Throwable) {
            null
        }
    }

    private fun synthToneSweep(duration: Float, startHz: Float, endHz: Float, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - t) * exp(-t * 2.5f)
            out[i] = (sin(phase) * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthNoiseSweep(duration: Float, startHz: Float, endHz: Float, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val noise = (Random.nextFloat() * 2f - 1f) * 0.55f
            val tone = sin(phase).toFloat() * 0.45f
            val env = (1f - t) * exp(-t * 3f)
            out[i] = ((noise + tone) * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthImpact(duration: Float, startHz: Float, endHz: Float, gain: Float, noiseMix: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * (t * 0.7f)
            phase += 2.0 * PI * freq / sampleRate
            val punch = sin(phase).toFloat()
            val crack = (Random.nextFloat() * 2f - 1f) * exp(-t * 14f) * noiseMix
            val env = exp(-t * 6.5f)
            val sample = (punch * env + crack) * gain
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthMetallic(duration: Float, f1: Float, f2: Float, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val s1 = sin(2.0 * PI * f1 * i / sampleRate).toFloat()
            val s2 = sin(2.0 * PI * f2 * i / sampleRate).toFloat()
            val env = exp(-t * 10f)
            out[i] = (((s1 + s2) * 0.5f) * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthLaserCharge(duration: Float, startHz: Float, endHz: Float, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * (t * t)
            phase += 2.0 * PI * freq / sampleRate
            val harmonic = sin(phase * 2.0).toFloat() * 0.35f
            val env = sin(t * PI).toFloat()
            out[i] = ((sin(phase).toFloat() + harmonic) * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthChord(duration: Float, freqs: FloatArray, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            var sum = 0f
            for (f in freqs) {
                sum += sin(2.0 * PI * f * i / sampleRate).toFloat()
            }
            val env = (1f - t) * exp(-t * 2f)
            out[i] = ((sum / freqs.size) * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthArpeggio(duration: Float, notes: FloatArray, gain: Float): ShortArray {
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        val noteLen = count / notes.size
        for (i in 0 until count) {
            val noteIdx = (i / noteLen).coerceAtMost(notes.lastIndex)
            val localT = (i % noteLen).toFloat() / noteLen
            val s = sin(2.0 * PI * notes[noteIdx] * i / sampleRate).toFloat()
            val env = exp(-localT * 3f)
            out[i] = (s * env * gain * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
