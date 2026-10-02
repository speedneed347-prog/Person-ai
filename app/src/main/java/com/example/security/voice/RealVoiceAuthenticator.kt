package com.example.security.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class VoiceVerificationOutput(
    val isOwnerVerified: Boolean,
    val similarityScore: Float,
    val threshold: Float,
    val message: String
)

class RealVoiceAuthenticator {

    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_SIZE = 512
        const val EMBEDDING_DIM = 64
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.78f
    }

    /**
     * Extracts acoustic feature embedding from a raw PCM audio buffer.
     */
    fun extractEmbedding(pcmData: ShortArray): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIM)
        if (pcmData.isEmpty()) return embedding

        val frames = pcmData.size / FRAME_SIZE
        if (frames == 0) return embedding

        // Compute Mel-frequency spectral distribution across frames
        val melBands = Array(EMBEDDING_DIM) { FloatArray(frames) }

        for (f in 0 until frames) {
            val offset = f * FRAME_SIZE
            val frameEnergy = FloatArray(FRAME_SIZE)
            var sumSquare = 0.0

            for (i in 0 until FRAME_SIZE) {
                // Apply Hamming window
                val window = 0.54 - 0.46 * cos(2.0 * Math.PI * i / (FRAME_SIZE - 1))
                val sample = pcmData[offset + i] * window
                frameEnergy[i] = sample.toFloat()
                sumSquare += sample * sample
            }

            // Energy threshold for speech activity detection
            val rms = sqrt(sumSquare / FRAME_SIZE)
            if (rms < 25.0) continue // Skip silent frames

            // Real Frequency-domain Spectral Filterbank via Discrete Fourier Transform
            val stepK = (FRAME_SIZE / 2) / EMBEDDING_DIM
            for (bin in 0 until EMBEDDING_DIM) {
                val centerFreqIndex = (bin + 1) * stepK
                var realPart = 0.0
                var imagPart = 0.0

                for (i in 0 until FRAME_SIZE) {
                    val angle = 2.0 * Math.PI * centerFreqIndex * i / FRAME_SIZE
                    val s = frameEnergy[i]
                    realPart += s * cos(angle)
                    imagPart -= s * kotlin.math.sin(angle)
                }

                val spectralPower = (realPart * realPart + imagPart * imagPart) / FRAME_SIZE
                melBands[bin][f] = ln(max(1.0, spectralPower)).toFloat()
            }
        }

        // Average across speech frames to generate mean log filterbank energy
        val meanMel = FloatArray(EMBEDDING_DIM)
        for (bin in 0 until EMBEDDING_DIM) {
            var sum = 0f
            var count = 0
            for (f in 0 until frames) {
                val v = melBands[bin][f]
                if (v > 0f) {
                    sum += v
                    count++
                }
            }
            meanMel[bin] = if (count > 0) sum / count else 0f
        }

        // Apply Cepstral Mean Subtraction and DCT-II to extract decorrelated MFCC coefficients
        val meanEnergy = meanMel.average().toFloat()
        for (n in 0 until EMBEDDING_DIM) {
            var sum = 0.0
            for (m in 0 until EMBEDDING_DIM) {
                sum += (meanMel[m] - meanEnergy) * cos(Math.PI * (n + 1) * (m + 0.5) / EMBEDDING_DIM)
            }
            embedding[n] = sum.toFloat()
        }

        // L2 Normalize embedding vector
        var norm = 0.0
        for (v in embedding) norm += v * v
        val l2 = sqrt(norm).toFloat()
        if (l2 > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= l2
            }
        }

        return embedding
    }

    /**
     * Calculates cosine similarity between two speaker embeddings.
     */
    fun computeCosineSimilarity(emb1: FloatArray, emb2: FloatArray): Float {
        if (emb1.size != emb2.size || emb1.isEmpty()) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in emb1.indices) {
            dot += emb1[i] * emb2[i]
            normA += emb1[i] * emb1[i]
            normB += emb2[i] * emb2[i]
        }

        val denom = sqrt((normA * normB).toDouble()).toFloat()
        return if (denom > 0f) (dot / denom).coerceIn(-1f, 1f) else 0f
    }

    /**
     * Verifies if live PCM voice matches the enrolled owner embedding.
     */
    fun verifySpeaker(
        liveAudioPcm: ShortArray,
        enrolledEmbedding: FloatArray,
        threshold: Float = DEFAULT_SIMILARITY_THRESHOLD
    ): VoiceVerificationOutput {
        if (enrolledEmbedding.isEmpty()) {
            return VoiceVerificationOutput(
                isOwnerVerified = false,
                similarityScore = 0f,
                threshold = threshold,
                message = "No owner voice profile enrolled."
            )
        }

        val liveEmbedding = extractEmbedding(liveAudioPcm)
        val similarity = computeCosineSimilarity(liveEmbedding, enrolledEmbedding)
        val isVerified = similarity >= threshold

        val msg = if (isVerified) {
            "Speaker identity verified (${(similarity * 100).toInt()}% match)."
        } else {
            "Speaker mismatch: ${(similarity * 100).toInt()}% match is below ${(threshold * 100).toInt()}% threshold."
        }

        return VoiceVerificationOutput(
            isOwnerVerified = isVerified,
            similarityScore = similarity,
            threshold = threshold,
            message = msg
        )
    }

    /**
     * Averages multiple enrolled sample vectors to produce an authoritative profile.
     */
    fun compileEnrolledEmbedding(samples: List<FloatArray>): FloatArray {
        if (samples.isEmpty()) return FloatArray(EMBEDDING_DIM)
        val compiled = FloatArray(EMBEDDING_DIM)

        for (sample in samples) {
            for (i in 0 until min(EMBEDDING_DIM, sample.size)) {
                compiled[i] += sample[i]
            }
        }

        var norm = 0.0
        for (i in compiled.indices) {
            compiled[i] /= samples.size
            norm += compiled[i] * compiled[i]
        }

        val l2 = sqrt(norm).toFloat()
        if (l2 > 0f) {
            for (i in compiled.indices) {
                compiled[i] /= l2
            }
        }

        return compiled
    }

    fun serializeEmbedding(embedding: FloatArray): String {
        return embedding.joinToString(",") { it.toString() }
    }

    fun deserializeEmbedding(serialized: String): FloatArray {
        if (serialized.isBlank()) return FloatArray(0)
        return try {
            serialized.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        } catch (_: Exception) {
            FloatArray(0)
        }
    }
}
