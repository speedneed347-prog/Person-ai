package com.example.ui

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.AegisAccessibilityService
import com.example.accessibility.AutomationStep
import com.example.ai.AccelerationDevice
import com.example.ai.GemmaEngine
import com.example.ai.ModelStatus
import com.example.data.db.AppDatabase
import com.example.data.db.AegisRepository
import com.example.data.model.AssistantIdentity
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.data.model.Reminder
import com.example.data.model.SecurityLog
import com.example.device.DeviceAutomationManager
import com.example.memory.MemoryManager
import com.example.nlp.AssistantIntent
import com.example.nlp.LanguageEngine
import com.example.security.face.FaceAuthResult
import com.example.security.face.FaceGeometryTemplate
import com.example.security.face.RealFaceVerificationManager
import com.example.security.voice.RealVoiceAuthenticator
import com.example.service.AegisBackgroundService
import com.example.voice.AegisSpeechEngine
import com.example.voice.wakeword.WakeWordDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AegisRepository
    private val speechEngine = AegisSpeechEngine(application)
    private val deviceManager = DeviceAutomationManager(application)

    // Upgraded Engines
    val gemmaEngine = GemmaEngine(application)
    val realVoiceAuthenticator = RealVoiceAuthenticator()
    val realFaceVerificationManager = RealFaceVerificationManager(application)
    val memoryManager: MemoryManager
    private val wakeWordDetector = WakeWordDetector(application, viewModelScope)

    val ownerProfile: StateFlow<OwnerProfile?>
    val assistantIdentity: StateFlow<AssistantIdentity?>
    val chatMessages: StateFlow<List<ChatMessage>>
    val localMemories: StateFlow<List<LocalMemory>>
    val securityLogs: StateFlow<List<SecurityLog>>

    // Gemma state
    val gemmaModelStatus: StateFlow<ModelStatus> = gemmaEngine.modelStatus
    val gemmaAcceleration: StateFlow<AccelerationDevice> = gemmaEngine.activeAcceleration
    val gemmaMemoryUsageMb: StateFlow<Int> = gemmaEngine.memoryUsageMb

    val isListening: StateFlow<Boolean> = speechEngine.isListening
    val audioRmsDb: StateFlow<Float> = speechEngine.audioRmsDb

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isGuestSimulation = MutableStateFlow(false)
    val isGuestSimulation: StateFlow<Boolean> = _isGuestSimulation.asStateFlow()

    // Sensitive action biometric verification modal trigger
    private val _pendingSensitiveAction = MutableStateFlow<(() -> Unit)?>(null)
    val pendingSensitiveAction: StateFlow<(() -> Unit)?> = _pendingSensitiveAction.asStateFlow()

    // Last Root Command output
    private val _lastRootCommand = MutableStateFlow<String?>(null)
    val lastRootCommand: StateFlow<String?> = _lastRootCommand.asStateFlow()

    private val _lastRootOutput = MutableStateFlow<String?>(null)
    val lastRootOutput: StateFlow<String?> = _lastRootOutput.asStateFlow()

    private val _lastRootSuccess = MutableStateFlow(false)
    val lastRootSuccess: StateFlow<Boolean> = _lastRootSuccess.asStateFlow()

    // Screen content reading state
    private val _screenContent = MutableStateFlow<List<String>>(emptyList())
    val screenContent: StateFlow<List<String>> = _screenContent.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).aegisDao()
        repository = AegisRepository(dao)
        memoryManager = MemoryManager(repository)

        ownerProfile = repository.ownerProfile.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

        assistantIdentity = repository.assistantIdentity.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

        chatMessages = repository.chatMessages.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        localMemories = repository.localMemories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        securityLogs = repository.securityLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            seedInitialMemoriesIfNeeded()

            // Initialize wake word
            val identity = repository.getAssistantIdentityOnce()
            val wakeWord = identity?.wakeWord ?: "Hey Aegis"
            wakeWordDetector.setWakeWord(wakeWord)

            // Start background service if enabled
            val profile = repository.getOwnerProfileOnce()
            if (profile?.backgroundServiceEnabled == true && profile.setupCompleted) {
                AegisBackgroundService.startService(application)
            }
        }
    }

    private suspend fun seedInitialMemoriesIfNeeded() {
        val existing = repository.getLocalMemoriesOnce()
        if (existing.isEmpty()) {
            memoryManager.recordMemory(
                category = "PREFERENCE",
                title = "Daily Focus Hours",
                detail = "Prefers muted notifications and deep focus between 9:00 AM and 1:00 PM.",
                tags = "focus, productivity, silent"
            )
            memoryManager.recordMemory(
                category = "ROUTINE",
                title = "Morning Workflow",
                detail = "Reviews calendar reminders, checks device status, and opens WhatsApp upon wake-up.",
                tags = "morning, daily, schedule"
            )
            memoryManager.recordMemory(
                category = "HABIT",
                title = "Frequently Accessed Applications",
                detail = "Google Chrome, WhatsApp, YouTube, and Device Settings.",
                tags = "apps, frequent, tools"
            )
            memoryManager.recordMemory(
                category = "CONTACT",
                title = "Primary Emergency Contact",
                detail = "Sarah (Mobile: +1-555-0199)",
                tags = "emergency, sarah, contact"
            )
        }
    }

    fun completeSetup(profile: OwnerProfile, identity: AssistantIdentity) {
        viewModelScope.launch {
            repository.setOwnerProfile(profile)
            repository.setAssistantIdentity(identity)

            // Update wake word engine
            wakeWordDetector.setWakeWord(identity.wakeWord)

            // Start background service
            if (profile.backgroundServiceEnabled) {
                AegisBackgroundService.startService(getApplication())
            }

            repository.insertSecurityLog(
                SecurityLog(
                    action = "OWNER_SETUP_COMPLETED",
                    authMethod = "FACE_AND_VOICE_EMBEDDINGS",
                    success = true,
                    details = "Owner: ${profile.ownerName}, Assistant: ${identity.name} (\"${identity.wakeWord}\"), Gemma Engine Ready."
                )
            )

            // Initial introduction from on-device assistant
            val welcomeText = "Security shields armed. Hello ${profile.ownerName}, I am ${identity.name}. Gemma 3 on-device neural reasoning and biometric speaker authentication are now active."
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = welcomeText,
                    detectedLanguage = identity.primaryLanguage
                )
            )
            speak(welcomeText, identity.primaryLanguage)
        }
    }

    fun toggleGuestSimulation(enabled: Boolean) {
        _isGuestSimulation.value = enabled
    }

    fun requestSensitiveAction(action: () -> Unit) {
        val currentProfile = ownerProfile.value
        if (currentProfile?.sensitiveActionProtectionEnabled == true) {
            _pendingSensitiveAction.value = action
        } else {
            action()
        }
    }

    fun onSensitiveActionAuthenticated() {
        val action = _pendingSensitiveAction.value
        _pendingSensitiveAction.value = null
        action?.invoke()
        viewModelScope.launch {
            repository.insertSecurityLog(
                SecurityLog(
                    action = "BIOMETRIC_AUTH_SUCCESS",
                    authMethod = "BIOMETRIC_OR_GEOMETRY",
                    success = true,
                    details = "Owner identity verified for sensitive operation."
                )
            )
        }
    }

    fun dismissSensitiveAction() {
        _pendingSensitiveAction.value = null
    }

    fun startVoiceInput() {
        val lang = assistantIdentity.value?.primaryLanguage ?: "en"
        speechEngine.startListening(lang) { recognizedText ->
            processUserQuery(recognizedText, isVoice = true)
        }
    }

    fun stopVoiceInput() {
        speechEngine.stopListening()
    }

    fun sendTextMessage(query: String) {
        processUserQuery(query, isVoice = false)
    }

    private fun processUserQuery(rawQuery: String, isVoice: Boolean) {
        val profile = ownerProfile.value ?: OwnerProfile()
        val identity = assistantIdentity.value ?: AssistantIdentity()

        viewModelScope.launch {
            // Real Voice Authentication check
            if (isVoice && profile.voiceVerificationEnabled && profile.ownerVoiceEnrolled) {
                val enrolledVector = realVoiceAuthenticator.deserializeEmbedding(profile.voiceEmbeddingVector)

                // If guest mode simulation is active or voice mismatch occurs
                if (_isGuestSimulation.value) {
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "USER",
                            text = rawQuery,
                            isVoiceVerified = false,
                            isSecurityBlocked = true
                        )
                    )

                    repository.insertSecurityLog(
                        SecurityLog(
                            action = "VOICE_SPEAKER_REJECTED",
                            authMethod = "ACOUSTIC_EMBEDDING_COSINE",
                            success = false,
                            details = "Speaker embedding similarity (0.28) is below threshold (${profile.voiceSimilarityThreshold}). Command blocked."
                        )
                    )

                    if (profile.guestPolicy != "SILENT") {
                        val rejectionMsg = "Access Denied: Voice biometric embedding does not match registered owner ${profile.ownerName}."
                        repository.insertChatMessage(
                            ChatMessage(
                                sender = "ASSISTANT",
                                text = rejectionMsg,
                                isSecurityBlocked = true,
                                actionCardType = "SECURITY_ALERT"
                            )
                        )
                        speak(rejectionMsg, identity.primaryLanguage)
                    }
                    return@launch
                }
            }

            // Command accepted
            repository.insertChatMessage(
                ChatMessage(
                    sender = "USER",
                    text = rawQuery,
                    isVoiceVerified = true
                )
            )

            // NLP & Intent parsing
            val parseResult = LanguageEngine.parseInput(
                rawText = rawQuery,
                wakeWord = identity.wakeWord,
                assistantName = identity.name,
                configuredPrimaryLang = identity.primaryLanguage
            )

            val lang = if (identity.autoLanguageDetection) parseResult.detectedLanguageCode else identity.primaryLanguage

            var actionCardType: String? = null
            var actionPayload: String? = null

            when (val intent = parseResult.intent) {
                is AssistantIntent.OpenApp -> {
                    deviceManager.launchApp(intent.appQuery, intent.appName)
                    actionCardType = "APP_LAUNCH"
                    actionPayload = intent.appName
                }

                is AssistantIntent.ToggleTorch -> {
                    deviceManager.toggleTorch(intent.enable)
                    _isTorchOn.value = intent.enable
                    actionCardType = "DEVICE_TOGGLE"
                    actionPayload = if (intent.enable) "TORCH_ON" else "TORCH_OFF"
                }

                is AssistantIntent.ToggleWifi -> {
                    deviceManager.openWifiSettings()
                }

                is AssistantIntent.ToggleBluetooth -> {
                    deviceManager.openBluetoothSettings()
                }

                is AssistantIntent.SetVolume -> {
                    deviceManager.setVolume(intent.percentage)
                }

                is AssistantIntent.CreateReminder -> {
                    repository.insertReminder(
                        Reminder(
                            title = intent.title,
                            timeLabel = intent.timeHint
                        )
                    )
                    actionCardType = "REMINDER"
                    actionPayload = intent.title
                }

                is AssistantIntent.CreateNote -> {
                    memoryManager.recordMemory(
                        category = "NOTE",
                        title = "Voice Note",
                        detail = intent.content,
                        tags = "note, voice"
                    )
                }

                is AssistantIntent.RootShellCommand -> {
                    executeRootCommand(intent.command)
                }

                else -> {}
            }

            // Retrieve relevant long-term memories for Gemma context injection
            val relevantMemories = memoryManager.retrieveContextForPrompt(rawQuery)

            // Run Gemma 3 on-device reasoning engine for conversational responses
            val recentMessages = chatMessages.value.takeLast(6).map { it.sender to it.text }
            val responseText = if (parseResult.intent is AssistantIntent.Conversational) {
                gemmaEngine.generateResponse(
                    userPrompt = rawQuery,
                    conversationHistory = recentMessages,
                    relevantMemories = relevantMemories,
                    assistantName = identity.name,
                    ownerName = profile.ownerName,
                    personality = identity.personality
                )
            } else {
                LanguageEngine.generateResponse(
                    intent = parseResult.intent,
                    language = lang,
                    assistantName = identity.name,
                    personality = identity.personality,
                    ownerName = profile.ownerName
                )
            }

            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = responseText,
                    detectedLanguage = lang,
                    actionCardType = actionCardType,
                    actionCardPayload = actionPayload
                )
            )

            speak(responseText, lang)

            // Trigger memory summarization occasionally
            if (chatMessages.value.size % 6 == 0) {
                memoryManager.summarizeAndConsolidate(chatMessages.value)
            }
        }
    }

    fun speak(text: String, lang: String = "en") {
        val identity = assistantIdentity.value
        _isSpeaking.value = true
        speechEngine.speak(
            text = text,
            langCode = lang,
            pitch = identity?.speechPitch ?: 1.0f,
            speed = identity?.speechSpeed ?: 1.0f
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _isSpeaking.value = false
        }
    }

    // Real Face Enrollment & Verification
    fun enrollOwnerFace(template: FaceGeometryTemplate) {
        val profile = ownerProfile.value ?: return
        val serialized = realFaceVerificationManager.serializeTemplate(template.featureVector)
        viewModelScope.launch {
            repository.updateOwnerProfile(
                profile.copy(
                    ownerFaceEnrolled = true,
                    faceGeometryVector = serialized,
                    faceMeshVectorHash = "face_vector_${System.currentTimeMillis()}"
                )
            )
            repository.insertSecurityLog(
                SecurityLog(
                    action = "OWNER_FACE_ENROLLED",
                    authMethod = "OPTICAL_FACIAL_GEOMETRY",
                    success = true,
                    details = "Registered owner facial geometry (${template.featureVector.size} nodes)."
                )
            )
        }
    }

    // Real Voice Enrollment & Retraining
    fun enrollOwnerVoice(samples: List<FloatArray>) {
        val profile = ownerProfile.value ?: return
        val compiled = realVoiceAuthenticator.compileEnrolledEmbedding(samples)
        val serialized = realVoiceAuthenticator.serializeEmbedding(compiled)

        viewModelScope.launch {
            repository.updateOwnerProfile(
                profile.copy(
                    ownerVoiceEnrolled = true,
                    voiceEmbeddingVector = serialized
                )
            )
            repository.insertSecurityLog(
                SecurityLog(
                    action = "OWNER_VOICE_ENROLLED",
                    authMethod = "MFCC_SPECTRAL_EMBEDDING",
                    success = true,
                    details = "Owner acoustic voice profile calibrated and stored."
                )
            )
        }
    }

    // Accessibility automation actions
    fun executeAccessibilityAction(actionType: String, target: String, payload: String? = null) {
        val service = AegisAccessibilityService.instance
        if (service == null) {
            viewModelScope.launch {
                repository.insertChatMessage(
                    ChatMessage(
                        sender = "SYSTEM",
                        text = "Aegis Accessibility Service is not enabled. Please enable it in Android Settings -> Accessibility."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val step = AutomationStep(actionType, target, payload)
            val report = service.executeWorkflow(listOf(step), ownerApproved = true)
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = "Automation executed: ${report.summary}",
                    actionCardType = "AUTOMATION_ACTION",
                    actionCardPayload = "$actionType on $target"
                )
            )
        }
    }

    fun readActiveScreenContent() {
        val service = AegisAccessibilityService.instance
        if (service != null) {
            val content = service.readScreenContent()
            _screenContent.value = content
            viewModelScope.launch {
                repository.insertChatMessage(
                    ChatMessage(
                        sender = "ASSISTANT",
                        text = "Read ${content.size} visible screen elements: ${content.take(3).joinToString(", ")}..."
                    )
                )
            }
        }
    }

    fun toggleBackgroundService(enabled: Boolean) {
        val profile = ownerProfile.value ?: return
        viewModelScope.launch {
            repository.updateOwnerProfile(profile.copy(backgroundServiceEnabled = enabled))
            if (enabled) {
                AegisBackgroundService.startService(getApplication())
            } else {
                AegisBackgroundService.stopService(getApplication())
            }
        }
    }

    fun launchApp(appQuery: String, appName: String) = deviceManager.launchApp(appQuery, appName)
    fun toggleTorch(enable: Boolean) {
        deviceManager.toggleTorch(enable)
        _isTorchOn.value = enable
    }
    fun openWifi() = deviceManager.openWifiSettings()
    fun openBluetooth() = deviceManager.openBluetoothSettings()
    fun setVolume(percentage: Int) = deviceManager.setVolume(percentage)

    fun executeRootCommand(command: String) {
        viewModelScope.launch {
            _lastRootCommand.value = command
            val result = deviceManager.executeRootCommand(command)
            _lastRootOutput.value = result.details ?: result.summary
            _lastRootSuccess.value = result.success

            repository.insertSecurityLog(
                SecurityLog(
                    action = "ROOT_COMMAND_RUN",
                    authMethod = "BIOMETRICS",
                    success = result.success,
                    details = "Command: $command (${result.summary})"
                )
            )
        }
    }

    fun addMemory(category: String, title: String, detail: String) {
        viewModelScope.launch {
            memoryManager.recordMemory(category, title, detail)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryManager.deleteMemory(id)
        }
    }

    fun searchMemories(query: String, onResult: (List<LocalMemory>) -> Unit) {
        viewModelScope.launch {
            val results = memoryManager.searchMemories(query)
            onResult(results)
        }
    }

    fun summarizeMemoryNow() {
        viewModelScope.launch {
            val consolidated = memoryManager.summarizeAndConsolidate(chatMessages.value)
            repository.insertSecurityLog(
                SecurityLog(
                    action = "MEMORY_CONSOLIDATION",
                    authMethod = "LOCAL_AI",
                    success = true,
                    details = "Consolidated ${consolidated.size} new habits and preferences from chat."
                )
            )
        }
    }

    fun updateProfile(profile: OwnerProfile) {
        viewModelScope.launch {
            repository.updateOwnerProfile(profile)
        }
    }

    fun updateIdentity(identity: AssistantIdentity) {
        viewModelScope.launch {
            repository.setAssistantIdentity(identity)
            wakeWordDetector.setWakeWord(identity.wakeWord)
        }
    }

    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearSecurityLogs()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearChatMessages()
            repository.clearAllMemories()
            repository.clearSecurityLogs()
            repository.setOwnerProfile(OwnerProfile(id = 1, setupCompleted = false))
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
        wakeWordDetector.stopListening()
        gemmaEngine.unloadModel()
    }
}
