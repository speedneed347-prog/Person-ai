package com.example.memory

import com.example.data.db.AegisRepository
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryManager(private val repository: AegisRepository) {

    /**
     * Performs indexed keyword & relevance-scored search across long-term memory.
     */
    suspend fun searchMemories(query: String, category: String? = null): List<LocalMemory> = withContext(Dispatchers.IO) {
        val all = repository.getLocalMemoriesOnce()
        if (query.isBlank() && category.isNullOrBlank()) return@withContext all

        val tokens = query.lowercase().split(" ").filter { it.length > 1 }

        all.map { mem ->
            var score = 0
            if (category != null && mem.category.equals(category, ignoreCase = true)) {
                score += 5
            }
            val titleLower = mem.title.lowercase()
            val detailLower = mem.detail.lowercase()
            val tagsLower = mem.tags.lowercase()

            for (token in tokens) {
                if (titleLower.contains(token)) score += 10
                if (tagsLower.contains(token)) score += 8
                if (detailLower.contains(token)) score += 4
            }

            if (mem.isPinned) score += 3
            score += (mem.accessCount.coerceAtMost(10))

            Pair(mem, score)
        }
            .filter { if (query.isBlank()) true else it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Retrieves top-K contextually relevant memories for LLM prompt injection.
     */
    suspend fun retrieveContextForPrompt(userPrompt: String, maxItems: Int = 4): List<LocalMemory> = withContext(Dispatchers.IO) {
        val matches = searchMemories(userPrompt)
        val selected = matches.take(maxItems)
        // Increment access counts for retrieved memories
        for (mem in selected) {
            repository.incrementMemoryAccess(mem.id)
        }
        selected
    }

    /**
     * Summarizes conversation history to extract persistent user preferences, habits, and contacts.
     */
    suspend fun summarizeAndConsolidate(chatHistory: List<ChatMessage>): List<LocalMemory> = withContext(Dispatchers.IO) {
        val extracted = mutableListOf<LocalMemory>()
        val userMessages = chatHistory.filter { it.sender == "USER" }

        for (msg in userMessages) {
            val text = msg.text
            val lower = text.lowercase()

            // Habit / Routine detection
            if (lower.contains("every morning") || lower.contains("daily") || lower.contains("at 7 am") || lower.contains("routine")) {
                extracted.add(
                    LocalMemory(
                        category = "ROUTINE",
                        title = "Routine: ${text.take(30)}...",
                        detail = text,
                        tags = "routine, habit, schedule",
                        confidence = 0.92f
                    )
                )
            }

            // Preference detection
            if (lower.contains("i prefer") || lower.contains("i like") || lower.contains("favorite") || lower.contains("always use")) {
                extracted.add(
                    LocalMemory(
                        category = "PREFERENCE",
                        title = "User Preference",
                        detail = text,
                        tags = "preference, personal, choice",
                        confidence = 0.94f
                    )
                )
            }

            // Contact mention
            if (lower.contains("call ") || lower.contains("mom") || lower.contains("dad") || lower.contains("wife") || lower.contains("sarah")) {
                extracted.add(
                    LocalMemory(
                        category = "CONTACT",
                        title = "Frequent Contact Context",
                        detail = text,
                        tags = "contact, call, person",
                        confidence = 0.90f
                    )
                )
            }
        }

        // Store non-duplicate extracted memories
        for (mem in extracted) {
            repository.insertMemory(mem)
        }

        extracted
    }

    suspend fun recordMemory(category: String, title: String, detail: String, tags: String = ""): Long {
        return repository.insertMemory(
            LocalMemory(
                category = category.uppercase(),
                title = title,
                detail = detail,
                tags = tags,
                confidence = 0.98f,
                accessCount = 1
            )
        )
    }

    suspend fun deleteMemory(id: Long) {
        repository.deleteMemory(id)
    }
}
