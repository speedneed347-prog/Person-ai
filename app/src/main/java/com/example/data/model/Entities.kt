package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String, // USER, ASSISTANT, SYSTEM
    val text: String,
    val detectedLanguage: String = "en",
    val isVoiceVerified: Boolean = true,
    val isSecurityBlocked: Boolean = false,
    val actionCardType: String? = null, // APP_LAUNCH, DEVICE_TOGGLE, REMINDER, NOTE, ROOT_COMMAND, SECURITY_ALERT
    val actionCardPayload: String? = null
)

@Entity(tableName = "local_memories")
data class LocalMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // PREFERENCE, ROUTINE, FREQUENT_APP, FAVORITE_CONTACT, HABIT, NOTE
    val title: String,
    val detail: String,
    val confidence: Float = 0.95f,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeLabel: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val priority: String = "NORMAL"
)

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val authMethod: String, // FACE, FINGERPRINT, VOICE, PIN, SYSTEM
    val success: Boolean,
    val details: String
)

@Entity(tableName = "automation_routines")
data class AutomationRoutine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerPhrase: String,
    val description: String,
    val iconName: String = "routine",
    val isRootRequired: Boolean = false,
    val isEnabled: Boolean = true
)
