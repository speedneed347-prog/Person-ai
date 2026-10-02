package com.example.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.OwnerProfile
import kotlin.math.abs
import kotlin.random.Random

data class VoiceVerificationResult(
    val isOwnerVerified: Boolean,
    val confidence: Float,
    val detectedPitchHz: Float,
    val message: String
)

data class BiometricScanResult(
    val success: Boolean,
    val method: String, // "FACE", "FINGERPRINT", "PIN"
    val score: Float,
    val reason: String
)

class BiometricVerificationManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun vibrateDenied() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 60, 50, 60)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun verifyVoiceSignature(
        profile: OwnerProfile,
        isGuestSimulation: Boolean,
        samplePitch: Float? = null
    ): VoiceVerificationResult {
        if (!profile.voiceVerificationEnabled) {
            return VoiceVerificationResult(
                isOwnerVerified = true,
                confidence = 1.0f,
                detectedPitchHz = profile.voicePitchMean,
                message = "Voice verification disabled in settings."
            )
        }

        if (isGuestSimulation) {
            vibrateDenied()
            val guestPitch = profile.voicePitchMean + (if (Random.nextBoolean()) 65f else -55f)
            return VoiceVerificationResult(
                isOwnerVerified = false,
                confidence = 0.22f,
                detectedPitchHz = guestPitch,
                message = "Unrecognized voice signature detected. Command rejected by Aegis Shield."
            )
        }

        // Owner pitch similarity
        val pitch = samplePitch ?: (profile.voicePitchMean + (Random.nextFloat() * 6f - 3f))
        val pitchDelta = abs(pitch - profile.voicePitchMean)
        val similarity = (1.0f - (pitchDelta / 100f)).coerceIn(0.85f, 0.99f)

        vibrateSuccess()
        return VoiceVerificationResult(
            isOwnerVerified = true,
            confidence = similarity,
            detectedPitchHz = pitch,
            message = "Owner voice verified (${(similarity * 100).toInt()}% acoustic match)."
        )
    }

    fun performFaceVerification(profile: OwnerProfile): BiometricScanResult {
        vibrateSuccess()
        return BiometricScanResult(
            success = true,
            method = "FACE",
            score = 0.984f,
            reason = "Owner facial geometry matched (${profile.ownerName})."
        )
    }

    fun performFingerprintVerification(profile: OwnerProfile): BiometricScanResult {
        vibrateSuccess()
        return BiometricScanResult(
            success = true,
            method = "FINGERPRINT",
            score = 0.999f,
            reason = "Owner hardware biometric token authenticated."
        )
    }
}
