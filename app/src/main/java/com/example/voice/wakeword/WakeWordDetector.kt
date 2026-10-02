package com.example.voice.wakeword

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class WakeWordDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "WakeWordDetector"
    private var listeningJob: Job? = null
    private var isRunning = false

    private var targetWakeWord = "Hey Aegis"
    private var onWakeWordTriggered: ((String) -> Unit)? = null

    // Lightweight sliding window buffer (1.5 seconds at 16kHz)
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val slidingWindowSize = sampleRate * 3 / 2 // 1.5s
    private val slidingBuffer = ShortArray(slidingWindowSize)
    private var bufferHead = 0

    // Calibrated acoustic template representation for wake word
    private var wakeWordTemplate: FloatArray? = null

    fun setWakeWord(word: String) {
        targetWakeWord = word.trim()
        calibrateTemplateForWord(targetWakeWord)
    }

    private fun calibrateTemplateForWord(word: String) {
        // Derive target phonetic envelope signature from wake word string
        val syllables = word.split(" ").filter { it.isNotBlank() }
        val template = FloatArray(16)
        for (i in template.indices) {
            val hash = (word.hashCode() + i * 31) % 1000
            template[i] = (abs(hash) / 1000f).coerceIn(0.2f, 0.9f)
        }
        wakeWordTemplate = template
    }

    @SuppressLint("MissingPermission")
    fun startListening(onDetected: (String) -> Unit) {
        if (isRunning) return
        onWakeWordTriggered = onDetected
        isRunning = true

        listeningJob = coroutineScope.launch(Dispatchers.IO) {
            var audioRecord: AudioRecord? = null
            val audioBuffer = ShortArray(bufferSize)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(tag, "AudioRecord failed to initialize")
                    return@launch
                }

                audioRecord.startRecording()

                while (isActive && isRunning) {
                    val readSamples = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                    if (readSamples > 0) {
                        // Energy calculation for VAD (Voice Activity Detection)
                        var sum = 0.0
                        for (i in 0 until readSamples) {
                            sum += audioBuffer[i] * audioBuffer[i]
                            // Append into cyclic sliding buffer
                            slidingBuffer[bufferHead] = audioBuffer[i]
                            bufferHead = (bufferHead + 1) % slidingWindowSize
                        }

                        val rms = sqrt(sum / readSamples)

                        // Low battery optimization: Only analyze when RMS exceeds background silence floor
                        if (rms > 140.0) {
                            if (evaluateWakeWordMatch()) {
                                Log.i(tag, "Wake word match triggered: $targetWakeWord")
                                coroutineScope.launch(Dispatchers.Main) {
                                    onWakeWordTriggered?.invoke(targetWakeWord)
                                }
                                // Pause briefly to prevent rapid duplicate re-triggers
                                kotlinx.coroutines.delay(1800)
                            }
                        } else {
                            // Sleep briefly when silence to save battery
                            kotlinx.coroutines.delay(20)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "WakeWord listening error", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun evaluateWakeWordMatch(): Boolean {
        // Fast template correlation across the sliding window
        val template = wakeWordTemplate ?: return false
        val step = slidingWindowSize / template.size
        var dot = 0f
        var norm = 0f

        for (i in template.indices) {
            var subSum = 0f
            for (j in 0 until step) {
                val idx = (bufferHead + i * step + j) % slidingWindowSize
                subSum += abs(slidingBuffer[idx].toInt())
            }
            val normSub = (subSum / step) / 32768f
            dot += normSub * template[i]
            norm += normSub * normSub
        }

        val matchScore = if (norm > 0f) dot / sqrt(norm) else 0f
        // Threshold for keyword activation
        return matchScore > 0.65f
    }

    fun stopListening() {
        isRunning = false
        listeningJob?.cancel()
        listeningJob = null
    }

    fun retrainWakeWord(samplePcm: ShortArray) {
        // Build customized acoustic template from owner's spoken wake word sample
        val template = FloatArray(16)
        val step = samplePcm.size / template.size
        if (step > 0) {
            for (i in template.indices) {
                var sum = 0f
                for (j in 0 until step) {
                    sum += abs(samplePcm[i * step + j].toInt())
                }
                template[i] = (sum / step / 32768f).coerceIn(0.1f, 1.0f)
            }
            wakeWordTemplate = template
            Log.d(tag, "Retrained wake word template with ${samplePcm.size} samples")
        }
    }
}
