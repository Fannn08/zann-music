package com.zann.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Efek untuk [SoundStyle]: reverb (Freeverb stereo), low-pass hangat untuk lo-fi dan panning berputar untuk 8D.
 * Tempo dan nada diurus PlaybackParameters (lihat PlaybackService), bukan di sini.
 * Hanya PCM 16-bit stereo; format lain lewat tanpa diubah (sama seperti SpatialAudioProcessor).
 */
@UnstableApi
class StyleAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var style: SoundStyle = SoundStyle.NORMAL
        set(value) {
            if (field != value) { field = value; resetPending = true }
        }

    @Volatile private var resetPending = false

    private var sampleRate = 44100
    private var reverb: Reverb? = null
    private var lowL = 0f
    private var lowR = 0f
    private var lfoPhase = 0.0
    private var delayBuf = FloatArray(0)
    private var delayPos = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        reverb = Reverb(sampleRate)
        delayBuf = FloatArray(((MAX_ITD_MS / 1000f) * sampleRate).roundToInt() + 4)
        onFlush()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val frames = inputBuffer.remaining() / BYTES_PER_FRAME
        if (frames == 0) { inputBuffer.position(inputBuffer.limit()); return }
        val out = replaceOutputBuffer(frames * BYTES_PER_FRAME)
        val current = style
        val rv = reverb
        if (resetPending) { resetPending = false; onFlush() }
        if (current == SoundStyle.NORMAL || current == SoundStyle.SPED_UP || current == SoundStyle.NIGHTCORE || rv == null) {
            val end = inputBuffer.position() + frames * BYTES_PER_FRAME
            out.put(inputBuffer.duplicate().apply { limit(end) })
            inputBuffer.position(inputBuffer.limit())
            out.flip()
            return
        }
        inputBuffer.order(ByteOrder.nativeOrder())
        out.order(ByteOrder.nativeOrder())

        // Parameter per gaya.
        val wet: Float; val room: Float; val damp: Float; val preMs: Float
        var lowpassHz = 0f
        var rotate = false
        when (current) {
            SoundStyle.SLOWED_REVERB -> { wet = 0.30f; room = 0.82f; damp = 0.45f; preMs = 12f }
            SoundStyle.LOFI -> { wet = 0.10f; room = 0.45f; damp = 0.7f; preMs = 0f; lowpassHz = 4200f }
            SoundStyle.EIGHT_D -> { wet = 0.14f; room = 0.6f; damp = 0.5f; preMs = 0f; rotate = true }
            else -> { wet = 0.34f; room = 0.92f; damp = 0.35f; preMs = 28f } // CONCERT
        }
        rv.set(room, damp, wet, preMs)
        val lpA = if (lowpassHz > 0f) 1f - exp(-2.0 * PI * lowpassHz / sampleRate).toFloat() else 1f
        val lfoStep = 2.0 * PI / (LFO_SECONDS * sampleRate)
        val dry = 1f - wet * 0.5f

        repeat(frames) {
            var l = inputBuffer.short / 32768f
            var r = inputBuffer.short / 32768f

            if (lowpassHz > 0f) {
                lowL += lpA * (l - lowL); lowR += lpA * (r - lowR)
                // Sedikit saturasi supaya terdengar hangat.
                l = tanh(lowL * 1.35f) / 1.35f
                r = tanh(lowR * 1.35f) / 1.35f
            }

            if (rotate) {
                val pan = sin(lfoPhase).toFloat()          // -1 kiri ... +1 kanan
                lfoPhase += lfoStep
                if (lfoPhase > 2 * PI) lfoPhase -= 2 * PI
                val angle = (pan + 1f) * (PI.toFloat() / 4f)
                val gl = cos(angle) * 1.3f
                val gr = sin(angle) * 1.3f
                // Beda waktu antar telinga: telinga yang jauh mendengar sedikit lebih lambat.
                val itd = abs(pan) * (MAX_ITD_MS / 1000f) * sampleRate
                val mono = (l + r) * 0.5f
                delayBuf[delayPos] = mono
                val far = readDelayed(itd)
                if (pan >= 0f) { l = far * gl; r = mono * gr } else { l = mono * gl; r = far * gr }
                delayPos = (delayPos + 1) % delayBuf.size
            }

            rv.process(l, r)
            l = l * dry + rv.outL
            r = r * dry + rv.outR
            out.putShort(LoudnessProcessor.toShort(l * OUTPUT))
            out.putShort(LoudnessProcessor.toShort(r * OUTPUT))
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    private fun readDelayed(samples: Float): Float {
        val size = delayBuf.size
        val pos = delayPos - samples
        val base = Math.floor(pos.toDouble()).toInt()
        val frac = pos - base
        val a = delayBuf[((base % size) + size) % size]
        val b = delayBuf[(((base + 1) % size) + size) % size]
        return a + (b - a) * frac
    }

    override fun onFlush() {
        lowL = 0f; lowR = 0f; lfoPhase = 0.0
        delayBuf.fill(0f); delayPos = 0
        reverb?.clear()
    }

    override fun onReset() {
        reverb = null
        delayBuf = FloatArray(0)
        lowL = 0f; lowR = 0f; lfoPhase = 0.0
    }

    /** Freeverb stereo: 8 comb + 4 allpass per kanal, dengan pre-delay. */
    private class Reverb(sampleRate: Int) {
        private val scale = sampleRate / 44100f
        private val combL = Array(COMB.size) { Comb((COMB[it] * scale).roundToInt().coerceAtLeast(8)) }
        private val combR = Array(COMB.size) { Comb(((COMB[it] + SPREAD) * scale).roundToInt().coerceAtLeast(8)) }
        private val apL = Array(ALL.size) { AllPass((ALL[it] * scale).roundToInt().coerceAtLeast(4)) }
        private val apR = Array(ALL.size) { AllPass(((ALL[it] + SPREAD) * scale).roundToInt().coerceAtLeast(4)) }
        private val pre = FloatArray((0.06f * sampleRate).roundToInt() + 2)
        private var prePos = 0
        private var preSamples = 0
        private var wet = 0.2f
        private val sr = sampleRate

        fun set(room: Float, damp: Float, wet: Float, preMs: Float) {
            val fb = 0.7f + room * 0.28f
            combL.forEach { it.feedback = fb; it.damp = damp }
            combR.forEach { it.feedback = fb; it.damp = damp }
            this.wet = wet
            preSamples = (preMs / 1000f * sr).roundToInt().coerceIn(0, pre.size - 1)
        }

        var outL = 0f
        var outR = 0f

        fun process(l: Float, r: Float) {
            pre[prePos] = (l + r) * 0.015f
            val idx = ((prePos - preSamples) % pre.size + pre.size) % pre.size
            val input = pre[idx]
            prePos = (prePos + 1) % pre.size
            var oL = 0f; var oR = 0f
            for (i in combL.indices) oL += combL[i].process(input)
            for (i in combR.indices) oR += combR[i].process(input)
            for (i in apL.indices) oL = apL[i].process(oL)
            for (i in apR.indices) oR = apR[i].process(oR)
            outL = oL * wet * 3f
            outR = oR * wet * 3f
        }

        fun clear() {
            combL.forEach { it.clear() }; combR.forEach { it.clear() }
            apL.forEach { it.clear() }; apR.forEach { it.clear() }
            pre.fill(0f)
        }

        private class Comb(size: Int) {
            private val buf = FloatArray(size)
            private var i = 0
            private var store = 0f
            var feedback = 0.84f
            var damp = 0.4f
            fun process(x: Float): Float {
                val o = buf[i]
                store = o * (1f - damp) + store * damp
                buf[i] = x + store * feedback
                i = (i + 1) % buf.size
                return o
            }
            fun clear() { buf.fill(0f); store = 0f; i = 0 }
        }

        private class AllPass(size: Int) {
            private val buf = FloatArray(size)
            private var i = 0
            fun process(x: Float): Float {
                val b = buf[i]
                val o = -x + b
                buf[i] = x + b * 0.5f
                i = (i + 1) % buf.size
                return o
            }
            fun clear() { buf.fill(0f); i = 0 }
        }

        private companion object {
            val COMB = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
            val ALL = intArrayOf(556, 441, 341, 225)
            const val SPREAD = 23
        }
    }

    private companion object {
        const val BYTES_PER_FRAME = 4
        const val OUTPUT = 0.9f
        const val LFO_SECONDS = 8.0
        const val MAX_ITD_MS = 0.6f
    }
}
