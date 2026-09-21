package com.novosynk.data.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.novosynk.logger.Logger
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

/**
 * In-app equalizer engine — replaces the old "open system equalizer" panel.
 *
 * Attaches Android's native audio effects to the *player's* audio session, so the EQ applies to
 * the entire app output (foreground, background service, notification audio) with no dependency
 * on a third-party EQ app.
 *
 * ## Why bass is done through the EQ curve, not BassBoost
 *
 * `android.media.audiofx.BassBoost` is a hardware effect that many devices do not implement.
 * Samsung flagships (verified on SM-S908E / S22 Ultra) report `strengthSupported == false`, so a
 * slider wired only to `BassBoost` moves a number and changes nothing audible. Bass is therefore
 * a **low-shelf on the EQ bands themselves** — the EQ is always present, so the boost is always
 * audible — and the hardware `BassBoost` is layered on *additionally* only where it supports a
 * strength. [bassAppliedViaEq] / [isBassBoostSupported] report which path is live.
 *
 * `CrossfadeExoPlayerAdapter` builds a fresh ExoPlayer per track, each with its own audio session
 * id, so [attach] runs on every track transition.
 */
internal class EqualizerEngine {

    /** Full engine state — mirror of the persisted settings. */
    data class State(
        val enabled: Boolean,
        val bands: List<Int>,
        val bass: Int,
        val treble: Int,
        val virtualizer: Int,
        val loudness: Int,
    ) {
        companion object {
            /** Unity curve at [BAND_COUNT] bands, everything off. */
            val DEFAULT =
                State(
                    enabled = false,
                    bands = List(BAND_COUNT) { 0 },
                    bass = 0,
                    treble = 0,
                    virtualizer = 0,
                    loudness = 0,
                )
        }
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null

    private var attachedSessionId: Int = Int.MIN_VALUE
    private var state: State = State.DEFAULT

    /** Band levels the device accepts, in millibels. Reported once an [Equalizer] exists. */
    var bandLevelRange: IntRange = BAND_LEVEL_MIN_MB..BAND_LEVEL_MAX_MB
        private set

    /** How many bands the device's EQ actually exposes (5 on most, 10 on some). */
    var deviceBandCount: Int = 0
        private set

    /** Centre frequency of each device band, Hz. Used to map the ISO curve onto the hardware. */
    var deviceBandFrequenciesHz: IntArray = IntArray(0)
        private set

    var isBassBoostSupported: Boolean = false
        private set

    var isVirtualizerSupported: Boolean = false
        private set

    /** True when a hardware BassBoost is doing part of the work alongside the EQ shelf. */
    var bassAppliedViaEq: Boolean = false
        private set

    /** True while effects are bound to a live audio session. */
    val isAttached: Boolean get() = equalizer != null

    /**
     * Device capability summary for logs/support. Empty when nothing is attached yet.
     */
    fun describeDevice(): String =
        "bands=$deviceBandCount freqs=${deviceBandFrequenciesHz.joinToString()} " +
            "rangeB=${bandLevelRange.first}..B${bandLevelRange.last} " +
            "hwBass=$isBassBoostSupported hwVirtualizer=$isVirtualizerSupported"

    /**
     * Attach the effect chain to [sessionId]. Safe to call repeatedly with the same id — only the
     * first call constructs anything. A previously attached session's effects are released.
     */
    fun attach(sessionId: Int) {
        if (sessionId == attachedSessionId && equalizer != null) {
            apply(state)
            return
        }
        release()
        try {
            equalizer =
                Equalizer(0, sessionId).also { eq ->
                    bandLevelRange = eq.bandLevelRange.let { it[0].toInt()..it[1].toInt() }
                    deviceBandCount = eq.numberOfBands.toInt()
                    deviceBandFrequenciesHz =
                        IntArray(deviceBandCount) { band ->
                            // audiofx returns centiHertz (1/100 Hz).
                            (eq.getCenterFreq(band.toShort()) / 100f).toInt()
                        }
                }
        } catch (e: Exception) {
            Logger.e(TAG, "Equalizer init failed: ${e.message}")
            equalizer = null
        }
        try {
            val bass = BassBoost(0, sessionId)
            // Samsung flagships report strengthSupported == false. Keep the instance only when
            // it can actually do something, so the UI/logs do not imply bass is hardware-backed.
            isBassBoostSupported = bass.strengthSupported
            if (isBassBoostSupported) {
                bassBoost = bass
            } else {
                bass.release()
                bassBoost = null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "BassBoost unavailable: ${e.message}")
            isBassBoostSupported = false
            bassBoost = null
        }
        try {
            val virt = Virtualizer(0, sessionId)
            isVirtualizerSupported = virt.strengthSupported
            if (isVirtualizerSupported) {
                virtualizer = virt
            } else {
                virt.release()
                virtualizer = null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Virtualizer unavailable: ${e.message}")
            isVirtualizerSupported = false
            virtualizer = null
        }
        try {
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
        } catch (e: Exception) {
            Logger.e(TAG, "LoudnessEnhancer unavailable: ${e.message}")
            loudnessEnhancer = null
        }
        attachedSessionId = sessionId
        apply(state)
        Logger.w(TAG, "Attached EQ to session $sessionId — ${describeDevice()}")
    }

    /**
     * Push [new] onto the attached effects.
     *
     * Order matters: audiofx effects ignore parameters set while disabled on several vendor
     * implementations, so the effect is enabled first and the curve written after. That ordering
     * bug is why bass previously appeared dead even on devices that do support the effect.
     */
    fun apply(new: State) {
        state = new
        val eq = equalizer ?: return

        try {
            eq.enabled = new.enabled
        } catch (e: Exception) {
            Logger.e(TAG, "EQ enable failed: ${e.message}")
        }

        try {
            val min = bandLevelRange.first
            val max = bandLevelRange.last
            val levels = renderBandLevels(new, min, max)
            for (band in levels.indices) {
                eq.setBandLevel(band.toShort(), levels[band].toShort())
            }
        } catch (e: Exception) {
            Logger.e(TAG, "setBandLevel failed: ${e.message}")
        }

        // Hardware bass is additive on top of the EQ shelf, and only where supported.
        bassAppliedViaEq = new.bass > 0
        bassBoost?.let { bass ->
            try {
                if (new.enabled && new.bass > 0) {
                    bass.setStrength(new.bass.coerceIn(0, 1000).toShort())
                    bass.enabled = true
                } else {
                    bass.enabled = false
                }
            } catch (e: Exception) {
                Logger.e(TAG, "BassBoost apply failed: ${e.message}")
            }
        }
        virtualizer?.let { virt ->
            try {
                if (new.enabled && new.virtualizer > 0) {
                    virt.setStrength(new.virtualizer.coerceIn(0, 1000).toShort())
                    virt.enabled = true
                } else {
                    virt.enabled = false
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Virtualizer apply failed: ${e.message}")
            }
        }
        loudnessEnhancer?.let { le ->
            try {
                if (new.enabled && new.loudness > 0) {
                    le.enabled = true
                    // Map 0-1000 to 0-3000mB (0 to +30dB)
                    le.setTargetGain(new.loudness * 3)
                } else {
                    le.enabled = false
                }
            } catch (e: Exception) {
                Logger.e(TAG, "LoudnessEnhancer apply failed: ${e.message}")
            }
        }
    }

    /** Detach and free every effect. Idempotent. */
    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (_: Exception) {
        }
        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Exception) {
        }
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        attachedSessionId = Int.MIN_VALUE
    }

    /**
     *
     * The UI speaks a fixed log-spaced 10-band curve so it looks identical on every device; the
     * hardware may expose 3, 5 or 10 bands at unrelated frequencies. Each device band takes the
     * curve interpolated at its own centre frequency, then the shelves are added:
     *
     * - **Bass** is a low shelf: full weight at/below [BASS_SHELF_FULL_HZ], tapering out by
     *   [BASS_SHELF_ZERO_HZ]. This is what makes the control actually audible.
     * - **Treble** mirrors it at the top end.
     *
     * Interpolating in log-frequency (not linear Hz) is what keeps the result musical: 31→62 Hz and
     * 8k→16k are both one octave, and a linear interpolation would smear the low end into mush.
     */
    private fun renderBandLevels(
        state: State,
        min: Int,
        max: Int,
    ): IntArray {
        val deviceFreqs = deviceBandFrequenciesHz
        if (deviceFreqs.isEmpty()) return IntArray(0)
        val bassDb = (state.bass.coerceIn(0, 1000) / 1000f) * BASS_MAX_DB
        val trebleDb = (state.treble.coerceIn(0, 1000) / 1000f) * TREBLE_MAX_DB
        return IntArray(deviceFreqs.size) { band ->
            val freq = deviceFreqs[band]
            val base = interpolateCurveAt(state.bands, freq)
            val shelf = (bassDb * bassShelfWeight(freq)) + (trebleDb * trebleShelfWeight(freq))
            (base + (shelf * 100f).toInt()).coerceIn(min, max)
        }
    }

    /** Curve value at [freq] Hz, linearly interpolated between the two bracketing ISO bands. */
    private fun interpolateCurveAt(
        bands: List<Int>,
        freq: Int,
    ): Int {
        if (bands.isEmpty() || freq <= 0) return 0
        val f = freq.toFloat()
        val firstF = BAND_CENTER_HZ.first().toFloat()
        val lastF = BAND_CENTER_HZ.last().toFloat()
        if (f <= firstF) return bands.first()
        if (f >= lastF) return bands.last()
        val pos = log2(f / firstF) / log2(lastF / firstF) * (BAND_CENTER_HZ.size - 1)
        val lower = pos.toInt().coerceIn(0, bands.size - 2)
        val t = pos - lower
        val a = bands[lower]
        val b = bands[lower + 1]
        return (a + (b - a) * t).toInt()
    }

    /** 1.0 at and below the bass shelf's full-boost point, easing to 0 by the zero point. */
    private fun bassShelfWeight(freqHz: Int): Float {
        if (freqHz <= BASS_SHELF_FULL_HZ) return 1f
        if (freqHz >= BASS_SHELF_ZERO_HZ) return 0f
        val t = (freqHz - BASS_SHELF_FULL_HZ).toFloat() / (BASS_SHELF_ZERO_HZ - BASS_SHELF_FULL_HZ)
        return 1f - t
    }

    /** Mirror of [bassShelfWeight] for the high end. */
    private fun trebleShelfWeight(freqHz: Int): Float {
        if (freqHz >= TREBLE_SHELF_FULL_HZ) return 1f
        if (freqHz <= TREBLE_SHELF_ZERO_HZ) return 0f
        val t =
            (freqHz - TREBLE_SHELF_ZERO_HZ).toFloat() /
                (TREBLE_SHELF_FULL_HZ - TREBLE_SHELF_ZERO_HZ)
        return t
    }

    companion object {
        private const val TAG = "EqualizerEngine"

        /** UI curve resolution: the ISO 10-band layout, so the screen is identical everywhere. */
        const val BAND_COUNT = 10

        /** UI slider range in millibels; the device clamps further at apply time. */
        const val BAND_LEVEL_MIN_MB = -1500
        const val BAND_LEVEL_MAX_MB = 1500

        /** Centre frequencies of the ISO 10-band layout, Hz. */
        val BAND_CENTER_HZ =
            intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        /** Full bass boost at the slider's maximum, in dB, shelf-weighted onto the low bands. */
        private const val BASS_MAX_DB = 15f

        /** Full treble boost at the slider's maximum, in dB. */
        private const val TREBLE_MAX_DB = 12f

        /** Bass shelf holds full weight up to here, then tapers out by [BASS_SHELF_ZERO_HZ]. */
        private const val BASS_SHELF_FULL_HZ = 125
        private const val BASS_SHELF_ZERO_HZ = 500

        /** Treble shelf mirror of the bass taper. */
        private const val TREBLE_SHELF_ZERO_HZ = 2000
        private const val TREBLE_SHELF_FULL_HZ = 8000

        /** Label for a band centre frequency: "31", "125", "1k", "3.6k". */
        fun bandLabel(centerHz: Int): String =
            if (centerHz >= 1000) {
                val k = centerHz / 1000f
                if (k % 1f == 0f) "${k.toInt()}k" else "${"%.1f".format(k)}k"
            } else {
                centerHz.toString()
            }

        /**
         * Named presets in millibels over the ISO 10-band layout, low → high. The classic
         * ten-band tables players have shipped for years, so they sound like what users expect.
         */
        val PRESETS: Map<String, List<Int>> =
            linkedMapOf(
                "Flat" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                "Acoustic" to listOf(450, 400, 350, 100, 100, 150, 250, 350, 350, 300),
                "Bass Boost" to listOf(650, 600, 500, 350, 200, 50, 0, 0, 0, 0),
                "Bass Reduce" to listOf(-650, -600, -500, -350, -200, -50, 0, 0, 0, 0),
                "Classical" to listOf(500, 450, 400, 300, 200, 150, 250, 350, 400, 450),
                "Dance" to listOf(600, 500, 400, 200, 50, 0, -100, 0, 200, 300),
                "Hip Hop" to listOf(650, 600, 450, 250, 100, 0, 50, 100, 200, 250),
                "Jazz" to listOf(400, 350, 250, 150, 50, 100, 200, 250, 300, 350),
                "Loudness" to listOf(650, 550, 350, 100, -100, -150, 0, 200, 400, 500),
                "Pop" to listOf(200, 300, 400, 400, 300, 150, 0, 100, 200, 300),
                "Rock" to listOf(600, 500, 350, 200, 50, 150, 300, 400, 450, 500),
                "Treble Boost" to listOf(0, 0, 0, 0, 0, 100, 250, 400, 550, 650),
                "Vocal" to listOf(-200, -250, -150, 50, 300, 450, 400, 250, 100, 0),
            )

        /** [bands] matches a built-in preset exactly? Returns its name for the UI, else null. */
        fun matchingPreset(bands: List<Int>): String? =
            PRESETS.entries.firstOrNull { (_, preset) ->
                preset.size == bands.size && preset == bands
            }?.key

        /** Octaves between two frequencies — used to keep the shelf tapers musically even. */
        fun octavesBetween(
            from: Int,
            to: Int,
        ): Float = abs(log2(to.toFloat() / from.toFloat()))
    }
}
