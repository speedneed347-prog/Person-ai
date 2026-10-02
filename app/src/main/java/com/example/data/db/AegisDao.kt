package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssistantIdentity
import com.example.data.model.AutomationRoutine
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.data.model.Reminder
import com.example.data.model.SecurityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AegisDao {

    // Owner Profile
    @Query("SELECT * FROM owner_profile WHERE id = 1 LIMIT 1")
    fun getOwnerProfile(): Flow<OwnerProfile?>

    @Query("SELECT * FROM owner_profile WHERE id = 1 LIMIT 1")
    suspend fun getOwnerProfileOnce(): OwnerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOwnerProfile(profile: OwnerProfile)

    @Update
    suspend fun updateOwnerProfile(profile: OwnerProfile)

    // Assistant Identity
    @Query("SELECT * FROM assistant_identity WHERE id = 1 LIMIT 1")
    fun getAssistantIdentity(): Flow<AssistantIdentity?>

    @Query("SELECT * FROM assistant_identity WHERE id = 1 LIMIT 1")
    suspend fun getAssistantIdentityOnce(): AssistantIdentity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAssistantIdentity(identity: AssistantIdentity)

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentChatMessagesOnce(limit: Int = 30): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // Local Memories
    @Query("SELECT * FROM local_memories ORDER BY isPinned DESC, accessCount DESC, updatedAt DESC")
    fun getLocalMemories(): Flow<List<LocalMemory>>

    @Query("SELECT * FROM local_memories ORDER BY isPinned DESC, accessCount DESC, updatedAt DESC")
    suspend fun getLocalMemoriesOnce(): List<LocalMemory>

    @Query("SELECT * FROM local_memories WHERE title LIKE '%' || :query || '%' OR detail LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<LocalMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: LocalMemory): Long

    @Query("UPDATE local_memories SET accessCount = accessCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementMemoryAccess(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM local_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM local_memories")
    suspend fun clearAllMemories()

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, timestampMillis ASC")
    fun getReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    // Security Logs
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 50")
    fun getSecurityLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearSecurityLogs()

    // Automation Routines
    @Query("SELECT * FROM automation_routines ORDER BY id ASC")
    fun getAutomationRoutines(): Flow<List<AutomationRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: AutomationRoutine)

    @Query("UPDATE automation_routines SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleRoutine(id: Long, enabled: Boolean)
}
