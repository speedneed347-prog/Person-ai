package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owner_profile")
data class OwnerProfile(
    @PrimaryKey val id: Int = 1,
    val ownerName: String = "Owner",
    val ownerFaceEnrolled: Boolean = false,
    val faceMeshVectorHash: String = "",
    val ownerFingerprintEnrolled: Boolean = false,
    val ownerVoiceEnrolled: Boolean = false,
    val voicePitchMean: Float = 165f, // typical pitch Hz
    val voiceEnergyVariance: Float = 0.42f,
    val voiceVerificationEnabled: Boolean = true,
    val strictOwnerOnlyMode: Boolean = true,
    val guestPolicy: String = "BLOCK", // BLOCK, GUEST_MODE, SILENT
    val sensitiveActionProtectionEnabled: Boolean = true,
    val localEncryptionEnabled: Boolean = true,
    val rootModeEnabled: Boolean = false,
    val setupCompleted: Boolean = false,
    val enrolledAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "assistant_identity")
data class AssistantIdentity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Aegis",
    val wakeWord: String = "Hey Aegis",
    val voiceStyle: String = "NEUTRAL", // MALE, FEMALE, NEUTRAL
    val personality: String = "GUARDIAN", // PROFESSIONAL, FRIENDLY, CASUAL, TECHNICAL, GUARDIAN
    val primaryLanguage: String = "en",
    val secondaryLanguage: String = "bn",
    val autoLanguageDetection: Boolean = true,
    val avatarStyle: String = "SHIELD", // SHIELD, CORE, ORB, HOLO, NEON
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f
)
