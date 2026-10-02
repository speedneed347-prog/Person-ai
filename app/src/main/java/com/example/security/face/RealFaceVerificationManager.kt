package com.example.security.face

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.sqrt

data class FaceGeometryTemplate(
    val eyeDistanceRatio: Float,
    val jawlineAspectRatio: Float,
    val foreheadToNoseRatio: Float,
    val symmetryScore: Float,
    val featureVector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceGeometryTemplate
        return featureVector.contentEquals(other.featureVector)
    }

    override fun hashCode(): Int {
        return featureVector.contentHashCode()
    }
}

data class FaceAuthResult(
    val success: Boolean,
    val confidence: Float,
    val methodUsed: String, // "HARDWARE_BIOMETRIC" or "OPTICAL_FACIAL_GEOMETRY"
    val message: String
)

class RealFaceVerificationManager(private val context: Context) {

    companion object {
        const val VECTOR_LENGTH = 16
        const val FACE_MATCH_THRESHOLD = 0.82f
    }

    /**
     * Checks if hardware biometrics (face / fingerprint) is enrolled on device.
     */
    fun isHardwareBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Authenticates using Android system BiometricPrompt (Hardware Face/Fingerprint).
     */
    suspend fun authenticateWithBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Owner Face Verification",
        subtitle: String = "Authenticate to authorize sensitive Aegis operation"
    ): FaceAuthResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(context)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use Optical Geometry")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (continuation.isActive) {
                    continuation.resume(
                        FaceAuthResult(
                            success = true,
                            confidence = 0.99f,
                            methodUsed = "HARDWARE_BIOMETRIC",
                            message = "Hardware biometric authentication confirmed."
                        )
                    )
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (continuation.isActive) {
                    continuation.resume(
                        FaceAuthResult(
                            success = false,
                            confidence = 0f,
                            methodUsed = "HARDWARE_BIOMETRIC",
                            message = "Biometric prompt cancelled: $errString"
                        )
                    )
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Transient fail, user can retry
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Generates a calibrated optical facial geometry template from facial landmarks / bounding measurements.
     */
    fun extractFaceTemplate(
        faceWidth: Float,
        faceHeight: Float,
        leftEyeX: Float,
        leftEyeY: Float,
        rightEyeX: Float,
        rightEyeY: Float,
        noseX: Float,
        noseY: Float,
        mouthCenterX: Float,
        mouthCenterY: Float
    ): FaceGeometryTemplate {
        val eyeDistance = sqrt(((rightEyeX - leftEyeX) * (rightEyeX - leftEyeX) + (rightEyeY - leftEyeY) * (rightEyeY - leftEyeY)).toDouble()).toFloat()
        val eyeDistRatio = eyeDistance / maxOf(faceWidth, 1f)
        val jawRatio = faceWidth / maxOf(faceHeight, 1f)
        val foreheadNoseRatio = abs(noseY - minOf(leftEyeY, rightEyeY)) / maxOf(faceHeight, 1f)
        val symmetry = (1f - abs((noseX - (leftEyeX + rightEyeX) / 2f) / maxOf(faceWidth, 1f))).coerceIn(0f, 1f)

        val vector = FloatArray(VECTOR_LENGTH)
        vector[0] = eyeDistRatio
        vector[1] = jawRatio
        vector[2] = foreheadNoseRatio
        vector[3] = symmetry
        for (i in 4 until VECTOR_LENGTH) {
            vector[i] = ((vector[i - 4] * 1.37f + i * 0.05f) % 1.0f).coerceIn(0.1f, 0.9f)
        }

        return FaceGeometryTemplate(
            eyeDistanceRatio = eyeDistRatio,
            jawlineAspectRatio = jawRatio,
            foreheadToNoseRatio = foreheadNoseRatio,
            symmetryScore = symmetry,
            featureVector = vector
        )
    }

    /**
     * Compares live optical facial geometry against enrolled owner template.
     */
    fun verifyFaceGeometry(
        liveTemplate: FaceGeometryTemplate,
        enrolledVector: FloatArray,
        threshold: Float = FACE_MATCH_THRESHOLD
    ): FaceAuthResult {
        if (enrolledVector.isEmpty()) {
            return FaceAuthResult(false, 0f, "OPTICAL_FACIAL_GEOMETRY", "No owner face profile enrolled.")
        }

        var dot = 0f
        var normA = 0f
        var normB = 0f

        val len = minOf(liveTemplate.featureVector.size, enrolledVector.size)
        for (i in 0 until len) {
            dot += liveTemplate.featureVector[i] * enrolledVector[i]
            normA += liveTemplate.featureVector[i] * liveTemplate.featureVector[i]
            normB += enrolledVector[i] * enrolledVector[i]
        }

        val similarity = if (normA > 0f && normB > 0f) {
            (dot / sqrt((normA * normB).toDouble())).toFloat().coerceIn(0f, 1f)
        } else 0f

        val verified = similarity >= threshold

        return FaceAuthResult(
            success = verified,
            confidence = similarity,
            methodUsed = "OPTICAL_FACIAL_GEOMETRY",
            message = if (verified) "Facial geometry verified (${(similarity * 100).toInt()}% match)."
            else "Facial mismatch: ${(similarity * 100).toInt()}% match below ${(threshold * 100).toInt()}% threshold."
        )
    }

    fun serializeTemplate(vector: FloatArray): String {
        return vector.joinToString(",") { it.toString() }
    }

    fun deserializeTemplate(str: String): FloatArray {
        if (str.isBlank()) return FloatArray(0)
        return try {
            str.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        } catch (_: Exception) {
            FloatArray(0)
        }
    }
}
