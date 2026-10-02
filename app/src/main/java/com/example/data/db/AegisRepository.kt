package com.example.data.db

import com.example.data.model.AssistantIdentity
import com.example.data.model.AutomationRoutine
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.data.model.Reminder
import com.example.data.model.SecurityLog
import kotlinx.coroutines.flow.Flow

class AegisRepository(private val dao: AegisDao) {

    val ownerProfile: Flow<OwnerProfile?> = dao.getOwnerProfile()
    val assistantIdentity: Flow<AssistantIdentity?> = dao.getAssistantIdentity()
    val chatMessages: Flow<List<ChatMessage>> = dao.getChatMessages()
    val localMemories: Flow<List<LocalMemory>> = dao.getLocalMemories()
    val reminders: Flow<List<Reminder>> = dao.getReminders()
    val securityLogs: Flow<List<SecurityLog>> = dao.getSecurityLogs()
    val automationRoutines: Flow<List<AutomationRoutine>> = dao.getAutomationRoutines()

    suspend fun getOwnerProfileOnce(): OwnerProfile? = dao.getOwnerProfileOnce()
    suspend fun setOwnerProfile(profile: OwnerProfile) = dao.setOwnerProfile(profile)
    suspend fun updateOwnerProfile(profile: OwnerProfile) = dao.updateOwnerProfile(profile)

    suspend fun getAssistantIdentityOnce(): AssistantIdentity? = dao.getAssistantIdentityOnce()
    suspend fun setAssistantIdentity(identity: AssistantIdentity) = dao.setAssistantIdentity(identity)

    suspend fun insertChatMessage(message: ChatMessage): Long = dao.insertChatMessage(message)
    suspend fun clearChatMessages() = dao.clearChatMessages()

    suspend fun insertMemory(memory: LocalMemory): Long = dao.insertMemory(memory)
    suspend fun deleteMemory(id: Long) = dao.deleteMemory(id)
    suspend fun clearAllMemories() = dao.clearAllMemories()

    suspend fun insertReminder(reminder: Reminder): Long = dao.insertReminder(reminder)
    suspend fun updateReminderStatus(id: Long, completed: Boolean) = dao.updateReminderStatus(id, completed)
    suspend fun deleteReminder(id: Long) = dao.deleteReminder(id)

    suspend fun insertSecurityLog(log: SecurityLog) = dao.insertSecurityLog(log)
    suspend fun clearSecurityLogs() = dao.clearSecurityLogs()

    suspend fun insertRoutine(routine: AutomationRoutine) = dao.insertRoutine(routine)
    suspend fun toggleRoutine(id: Long, enabled: Boolean) = dao.toggleRoutine(id, enabled)

    suspend fun initializeDefaultsIfNeeded() {
        if (dao.getOwnerProfileOnce() == null) {
            dao.setOwnerProfile(
                OwnerProfile(
                    id = 1,
                    ownerName = "Alex Vance",
                    ownerFaceEnrolled = false,
                    ownerFingerprintEnrolled = false,
                    ownerVoiceEnrolled = false,
                    voiceVerificationEnabled = true,
                    strictOwnerOnlyMode = true,
                    guestPolicy = "BLOCK",
                    sensitiveActionProtectionEnabled = true,
                    localEncryptionEnabled = true,
                    rootModeEnabled = false,
                    setupCompleted = false
                )
            )
        }

        if (dao.getAssistantIdentityOnce() == null) {
            dao.setAssistantIdentity(
                AssistantIdentity(
                    id = 1,
                    name = "Aegis",
                    wakeWord = "Hey Aegis",
                    voiceStyle = "NEUTRAL",
                    personality = "GUARDIAN",
                    primaryLanguage = "en",
                    secondaryLanguage = "bn",
                    autoLanguageDetection = true,
                    avatarStyle = "SHIELD",
                    speechSpeed = 1.0f,
                    speechPitch = 1.0f
                )
            )
        }
    }
}
